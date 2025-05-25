package Namingserver.namingserver.controller.Agent;

import Namingserver.namingserver.controller.ServerController;
import Namingserver.namingserver.controller.communication.ServerUnicastSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FailureAgent implements Runnable {

    private final ServerController controller;
    private final int failedPort;

    public FailureAgent(ServerController controller, int failedPort) {
        this.controller = controller;
        this.failedPort = failedPort;
    }

    @Override
    public void run() {
        System.out.println("FailureAgent started for port " + failedPort);

        Integer failedHash = controller.getNodeMap().entrySet().stream()
                .filter(e -> e.getValue().equals(failedPort))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (failedHash == null) {
            System.err.println("❌ Failed node not found in map (port " + failedPort + ")");
            return;
        }

        System.out.println("🚨 Handling failure for node on port " + failedPort + " (hash " + failedHash + ")");

        // Behandel lokale bestanden van de gefaalde node
        List<String> lostFiles = controller.getLocalFiles().remove(failedHash);
        System.err.println("Lost files for failed node " + failedHash + ": " + lostFiles);

        if (lostFiles != null) {
            for (String file : lostFiles) {
                Integer currentOwner = controller.getFileToNodeMap().get(file);
                System.err.println("Checking local file '" + file + "' - currentOwner: " + currentOwner + ", failedHash: " + failedHash);

                if (currentOwner != null && currentOwner.equals(failedHash)) {
                    Optional<Map.Entry<Integer, List<String>>> replicaHolder = controller.getReplicas().entrySet().stream()
                            .filter(e -> e.getValue().contains(file))
                            .findFirst();

                    if (replicaHolder.isPresent()) {
                        Integer newOwnerHash = replicaHolder.get().getKey();

                        // Promote replica naar nieuwe eigenaar
                        controller.getFileToNodeMap().put(file, newOwnerHash);
                        controller.getLocalFiles().computeIfAbsent(newOwnerHash, k -> new ArrayList<>()).add(file);

                        // Verwijder file uit replica lijst van nieuwe eigenaar
                        controller.getReplicas().get(newOwnerHash).remove(file);

                        // Zoek nieuwe replica (anders dan nieuwe eigenaar)
                        int fileHash = Functions.HashingFunction.hashNodeName(file);
                        Integer newReplica = controller.getNodeMap().floorKey(fileHash);
                        if (newReplica == null) newReplica = controller.getNodeMap().lastKey();

                        if (!newReplica.equals(newOwnerHash)) {
                            controller.getReplicas().computeIfAbsent(newReplica, k -> new ArrayList<>()).add(file);

                            int fromPort = controller.getNodeMap().get(newOwnerHash);
                            int toPort = controller.getNodeMap().get(newReplica);
                            ServerUnicastSender.sendReplicaInstruction(String.valueOf(fromPort), file, String.valueOf(toPort));

                            // Nieuwe replica maken van de gepromote eigenaar
                            Integer nextReplica = controller.getNodeMap().higherKey(newOwnerHash);
                            if (nextReplica == null) nextReplica = controller.getNodeMap().firstKey();
                            if (!nextReplica.equals(newOwnerHash)) {
                                controller.getReplicas().computeIfAbsent(nextReplica, k -> new ArrayList<>()).add(file);
                                int nextReplicaPort = controller.getNodeMap().get(nextReplica);
                                ServerUnicastSender.sendReplicaInstruction(String.valueOf(fromPort), file, String.valueOf(nextReplicaPort));
                                System.out.println("🔄 Created new replica of file '" + file + "' from owner " + newOwnerHash + " to " + nextReplica);
                            }
                        }

                        System.out.println("✅ Local file '" + file + "' recovered via replica (new owner hash: " + newOwnerHash + ")");
                    } else {
                        System.err.println("❌ Local file '" + file + "' lost — no replica available");
                        controller.getFileToNodeMap().remove(file);
                    }
                }
            }
        }

        // Behandel replica-bestanden van de gefaalde node (nieuw replica toewijzen)
        List<String> replicaFiles = controller.getReplicas().get(failedHash);
        System.err.println("Replica files of failed node: " + replicaFiles);

        if (replicaFiles != null) {
            for (String file : replicaFiles) {
                Integer ownerHash = controller.getFileToNodeMap().get(file);
                if (ownerHash == null) continue; // eigenaar onbekend, overslaan

                int fileHash = Functions.HashingFunction.hashNodeName(file);

                // Zoek nieuwe replica node (niet eigenaar, niet gefaalde node)
                Integer newReplica = null;
                for (Integer candidate : controller.getNodeMap().tailMap(fileHash + 1).keySet()) {
                    if (!candidate.equals(ownerHash) && !candidate.equals(failedHash)) {
                        newReplica = candidate;
                        break;
                    }
                }
                if (newReplica == null) {
                    for (Integer candidate : controller.getNodeMap().keySet()) {
                        if (!candidate.equals(ownerHash) && !candidate.equals(failedHash)) {
                            newReplica = candidate;
                            break;
                        }
                    }
                }

                if (newReplica != null) {
                    controller.getReplicas().computeIfAbsent(newReplica, k -> new ArrayList<>()).add(file);

                    int fromPort = controller.getNodeMap().get(ownerHash);
                    int toPort = controller.getNodeMap().get(newReplica);
                    ServerUnicastSender.sendReplicaInstruction(String.valueOf(fromPort), file, String.valueOf(toPort));

                    System.out.println("Created new replica for file '" + file + "' at node hash " + newReplica);
                } else {
                    System.err.println("No suitable new replica node found for file '" + file + "'");
                }
            }
        }

        // Verwijder gefaalde node pas aan het einde
        controller.getNodeMap().remove(failedHash);
        controller.getReplicas().remove(failedHash);
        System.err.println("Node " + failedHash + " removed from nodeMap and replicas.");

        System.out.println("🧹 Failure cleanup done for port " + failedPort);
    }
}
