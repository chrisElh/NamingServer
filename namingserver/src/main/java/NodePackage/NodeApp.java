package NodePackage;

import NodePackage.communication.*;
import Functions.HashingFunction;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NodeApp {
    private final List<int[]> neighborCandidates = new ArrayList<>();

    // Creates a new node, starts listening on its own port, and announces itself via multicast
    public Node createAndAnnounceNewNode(String name, int unicastPort, String dirPathLocal, String dirPathReplica) {
        Node node = new Node(name, unicastPort);


        node.loadLocalFilesFromDirectory(dirPathLocal);
        System.out.println("From NodeApp: " + node.getLocalFileNames());


        try {
            // Start the UDP unicast receiver so this node can receive messages (e.g., nodeCount, neighbors)
            startUnicastReceiver(node);

            //Laat deze node luisteren naar binnenkomende TCP-bestanden op zijn eigen poort.
            new Thread(new FileReceiver(node.getPort(), dirPathReplica, node)).start();


            // Broadcast this node's presence using multicast
            MulticastSender.sendMulticast(name, unicastPort, node.getLocalFileNames());

            new Thread(new MulticastReceiver(node)).start();

            // we start the watcher to detect when the files are changed
            new Thread(new FileWatcher(node, dirPathLocal + name)).start();

        } catch (Exception e) {
            System.err.println("Error while sending multicast:");
            e.printStackTrace();
        }

        return node;
    }



    // Start een thread die luistert op de eigen poort van de node en berichten verwerkt
    private void startUnicastReceiver(Node node) {
        // Definieer de message handler die inkomende berichten verwerkt
        UnicastReceiver.MessageHandler handler = message -> {
            System.out.println("Node " + node.getName() + " received message: " + message);
            System.out.println(">> Aangekomen bij addNodeFromMulticast");

            try {
                // ------------------------------
                // 1. Check of het om een REPLICA-instructie gaat
                // Formaat verwacht: "REPLICA:bestandsnaam:poort"
                // ------------------------------
                if (message.startsWith("REPLICA:")) {
                    String[] replicaParts = message.split(":");
                    if (replicaParts.length == 3) {
                        String filename = replicaParts[1];
                        int targetPort = Integer.parseInt(replicaParts[2]);

                        System.out.println("Instruction received to send the file : " + filename + " → port " + targetPort);

                        // Opbouw van pad naar het bestand dat verzonden moet worden
//                        String filePath = "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\" + filename;

                        File file = node.findFileByName(node.getLocalFileObjects(), filename);
                        // Verstuur bestand via TCP naar opgegeven poort
                        FileSender.sendFile(file, targetPort);
                    } else {
                        System.err.println("Invalid REPLICA message format: " + message);
                    }
                }
                // ------------------------------
                // 2. Alle andere berichten worden via komma gesplitst
                // ------------------------------
                else {
                    String[] parts = message.trim().split(",");

                    // ------------------------------
                    // 2.a Enkel totaal aantal nodes (bijv. "2")
                    // → Als node alleen is: vorige en volgende = zichzelf
                    // ------------------------------
                    if (parts.length == 1) {
                        int total = Integer.parseInt(parts[0]);

                        if (total < 1) {
                            int hash = HashingFunction.hashNodeName(node.getName());
                            node.setPreviousID(hash);
                            node.setNextID(hash);
                            System.out.println("Solo node → previousID and nextID set to own hash: " + hash);
                        }

                        node.setTotalNodes(total);
                        System.out.println("Updated totalNodes to: " + total);
                    }

                    // ------------------------------
                    // 2.b Bureninformatie ontvangen van andere node (bijv. "2269,3030")
                    // ------------------------------
                    else if (parts.length == 2) {
                        int sender = Integer.parseInt(parts[0]);
                        int other = Integer.parseInt(parts[1]);
                        neighborCandidates.add(new int[]{sender, other});

                        System.out.println("Received neighbor candidate: " + sender + ", " + other);

                        // Als we voldoende bureninformatie verzameld hebben
                        if (neighborCandidates.size() >= node.getTotalNodes() - 1) {
                            decideNeighbors(node);
                        }
                    }

                    // ------------------------------
                    // 2.c Ongeldig formaat
                    // ------------------------------
                    else {
                        System.err.println("Unknown message format: " + message);
                    }
                }
            } catch (NumberFormatException e) {
                // Fout bij het omzetten van string naar int
                System.err.println("Invalid number format in message: " + message);
            }
        };

        // Start de thread die de unicastberichten op de poort van deze node verwerkt
        Thread receiverThread = new Thread(new UnicastReceiver(node.getPort(), handler));
        receiverThread.start();
    }

    private void decideNeighbors(Node node) {
        int selfHash = HashingFunction.hashNodeName(node.getName());
        int bestPrev = -1;
        int bestNext = -1;
        int minPrevDistance = Integer.MAX_VALUE;
        int minNextDistance = Integer.MAX_VALUE;

        for (int[] pair : neighborCandidates) {
            int candidate = pair[0]; // hash van andere node

            int diff = Math.floorMod(selfHash - candidate, Integer.MAX_VALUE);
            if (diff > 0 && diff < minPrevDistance) {
                minPrevDistance = diff;
                bestPrev = candidate;
            }

            diff = Math.floorMod(candidate - selfHash, Integer.MAX_VALUE);
            if (diff > 0 && diff < minNextDistance) {
                minNextDistance = diff;
                bestNext = candidate;
            }
        }

        node.setPreviousID(bestPrev);
        node.setNextID(bestNext);
        System.out.println("Chosen neighbors for " + node.getName() + " → prevID: " + bestPrev + ", nextID: " + bestNext);
    }

}





