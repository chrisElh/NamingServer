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
    public Node createAndAnnounceNewNode(String name, int unicastPort) {
        Node node = new Node(name, unicastPort);




        try {
            // Start the UDP unicast receiver so this node can receive messages (e.g., nodeCount, neighbors)
            startUnicastReceiver(node);

            //Laat deze node luisteren naar binnenkomende TCP-bestanden op zijn eigen poort.
            new Thread(new FileReceiver(node.getPort(), "./data")).start();


            // Broadcast this node's presence using multicast
            MulticastSender.sendMulticast(name, unicastPort);

            new Thread(new MulticastReceiver(node)).start();

        } catch (Exception e) {
            System.err.println("Error while sending multicast:");
            e.printStackTrace();
        }

        return node;
    }

    // Start een thread die luistert op de eigen poort van de node en berichten verwerkt
    private void startUnicastReceiver(Node node) {
        UnicastReceiver.MessageHandler handler = message -> {
            System.out.println("Node " + node.getName() + " received message: " + message);

            String[] parts = message.trim().split(",");
            System.out.println(">> Aangekomen bij addNodeFromMulticast");


            try {
                if (parts.length == 1) {
                    // Slide 4
                    int total = Integer.parseInt(parts[0]);

                    if (total < 1) {
                        int hash = HashingFunction.hashNodeName(node.getName());
                        node.setPreviousID(hash);
                        node.setNextID(hash);
                        System.out.println("Solo node → previousID and nextID set to own hash: " + hash);
                    }

                    node.setTotalNodes(total);
                    System.out.println("Updated totalNodes to: " + total);

                } else if (parts.length == 2) {
                    // Slide 5
                    int sender = Integer.parseInt(parts[0]);
                    int other = Integer.parseInt(parts[1]);
                    neighborCandidates.add(new int[]{sender, other});

                    System.out.println("Received neighbor candidate: " + sender + ", " + other);

                    // Als we genoeg reacties hebben (optioneel: wachten op totalNodes - 1)
                    if (neighborCandidates.size() >= node.getTotalNodes() - 1) {
                        decideNeighbors(node);
                    }


                }


                else if (parts[0].startsWith("REPLICA")) {
                    String[] replicaParts = message.split(":");
                    if (replicaParts.length == 3) {
                        String filename = replicaParts[1];
                        int targetPort = Integer.parseInt(replicaParts[2]);

                        System.out.println("Instroction received to send the file : " + filename + " → port " + targetPort);

                        // Pad opbouwen naar bestand
                        String filePath = "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources" + filename;

                        // Bestand verzenden via TCP
                        FileSender.sendFile(filePath, targetPort);
                    } else {
                        System.err.println("Invalid message format: " + message);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in message: " + message);
            }
        };

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





