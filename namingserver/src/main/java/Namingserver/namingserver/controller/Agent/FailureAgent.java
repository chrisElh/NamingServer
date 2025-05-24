package Namingserver.namingserver.controller.Agent;


import Namingserver.namingserver.controller.ServerController;
import Namingserver.namingserver.controller.communication.ServerUnicastSender;

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

        // Hier komt de oude handleFailure-logica (aangepast om controller te gebruiken)

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

        controller.getNodeMap().remove(failedHash);
        List<String> lostFiles = controller.getLocalFiles().remove(failedHash);
        controller.getReplicas().remove(failedHash);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+lostFiles);

        if (lostFiles != null) {
            System.out.println("Lost files for failed node " + failedHash + ": " + lostFiles);

            for (String file : lostFiles) {
                Integer currentOwner = controller.getFileToNodeMap().get(file);
                System.err.println("Checking file '" + file + "' - currentOwner: " + currentOwner + ", failedHash: " + failedHash);

                if (currentOwner != null && currentOwner.equals(failedHash)) {
                    Optional<Map.Entry<Integer, List<String>>> replicaHolder = controller.getReplicas().entrySet().stream()
                            .filter(e -> e.getValue().contains(file))
                            .findFirst();



                    //0488518773

                    if (replicaHolder.isPresent()) {
                        Integer newOwnerHash = replicaHolder.get().getKey();

                        controller.getFileToNodeMap().put(file, newOwnerHash);
                        controller.getLocalFiles().computeIfAbsent(newOwnerHash, k -> new java.util.ArrayList<>()).add(file);

                        controller.getReplicas().get(newOwnerHash).remove(file);

                        int fileHash = Functions.HashingFunction.hashNodeName(file);
                        Integer newReplica = controller.getNodeMap().floorKey(fileHash);
                        if (newReplica == null) newReplica = controller.getNodeMap().lastKey();

                        if (!newReplica.equals(newOwnerHash)) {
                            controller.getReplicas().computeIfAbsent(newReplica, k -> new java.util.ArrayList<>()).add(file);

                            int fromPort = controller.getNodeMap().get(newOwnerHash);
                            int toPort = controller.getNodeMap().get(newReplica);

                            ServerUnicastSender.sendReplicaInstruction(String.valueOf(fromPort), file, String.valueOf(toPort));
                        }

                        System.out.println("✅ File '" + file + "' recovered via replica (new owner hash: " + newOwnerHash + ")");
                    } else {
                        System.err.println("❌ File '" + file + "' lost — no replica available");
                        controller.getFileToNodeMap().remove(file);
                    }
                }
            }
        }

        System.out.println("🧹 Failure cleanup done for port " + failedPort);
    }
}
