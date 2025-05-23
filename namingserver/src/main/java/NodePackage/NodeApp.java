package NodePackage;

import NodePackage.communication.*;
import Functions.HashingFunction;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NodeApp {
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private static final String NAMING_BASE = "http://localhost:8080";
    private final List<int[]> neighborCandidates = new ArrayList<>();

    public Node createAndAnnounceNewNode(String name, int unicastPort, String dirPathLocal, String dirPathReplica) {
        Node node = new Node(name, unicastPort);
        node.loadLocalFilesFromDirectory(dirPathLocal);
        System.out.println("From NodeApp: " + node.getLocalFileNames());

        try {
            // Open gedeelde TCP-server
            ServerSocket sharedSocket = new ServerSocket(unicastPort);
            System.out.println("✔️ TCP server socket opened on port " + unicastPort);

            // Maak file receiver aan (zonder eigen socket)
            FileReceiver fileReceiver = new FileReceiver(dirPathReplica, node);

            // Start TCP handler die alles verwerkt
            new Thread(new TCPMessageHandler(sharedSocket, node, this, fileReceiver)).start();

            // Start UDP en andere componenten
            startUnicastReceiver(node);
            MulticastSender.sendMulticast(name, unicastPort, node.getLocalFileNames());
            new Thread(new MulticastReceiver(node, this)).start();
            new Thread(new FileWatcher(node, dirPathLocal + name)).start();


        } catch (Exception e) {
            System.err.println("Error while sending multicast:");
            e.printStackTrace();
        }
        return node;
    }

    /**
     * Start een UnicastReceiver (UDP) op de poort van de node.
     * Verwerkt enkel REPLICA-commando’s en neighbor hash-informatie.
     * 'PING' en failure detection gebeuren elders (via TCP).
     */
    private void startUnicastReceiver(Node node) {
        UnicastReceiver.MessageHandler handler = message -> {
            System.out.println("Node " + node.getName() + " received message: " + message);

            try {
                // 🟣 1. Verwerk replica-verzoek
                if (message.startsWith("REPLICA:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String filename = parts[1];
                        int targetPort = Integer.parseInt(parts[2]);
                        File file = node.findFileByName(node.getLocalFileObjects(), filename);
                        FileSender.sendFile(file, targetPort);
                    }

                    // 🟡 2. Verwerk neighborinformatie uit multicast-response
                } else {

                    message = message.trim();

                    // Handle new control messages for shutdown
                    if (message.startsWith("UPDATE_NEXT:")) {
                        int newNextPort = Integer.parseInt(message.split(":")[1]);
                        node.setNextPort(newNextPort);
                        System.out.println("→ Updated NEXT port to " + newNextPort);
                        return;
                    }

                    if (message.startsWith("UPDATE_PREV:")) {
                        int newPrevPort = Integer.parseInt(message.split(":")[1]);
                        node.setPreviousPort(newPrevPort);
                        System.out.println("→ Updated PREVIOUS port to " + newPrevPort);
                        return;
                    }
                    String[] parts = message.trim().split(",");

                    System.out.println("WE KOMEN NET VOOR DE PART SPLITSING!!!!!!!!!!!!!!!!!1");

                    // ⬅️ Alleen totaal aantal nodes (bv. "2")
                    if (parts.length == 1) {
                        System.out.println("PAAAAART1!!!!!!!!!!!!!11");
                        int total = Integer.parseInt(parts[0]);

                        // Alleen node → zichzelf instellen als eigen neighbor
                        if (total < 1) {
                            int hash = HashingFunction.hashNodeName(node.getName());
                            node.setPreviousID(hash);
                            node.setNextID(hash);
                            System.out.println("Solo node → previousID and nextID set to own hash: " + hash);
                        }

                        node.setTotalNodes(total);
                        System.out.println("Updated totalNodes to: " + total);
                    }

                    else if (parts.length == 2) {
                        System.out.println("PAAAAART2222!!!!!!!!!!!!!11");
                        int sender = Integer.parseInt(parts[0]);
                        int other = Integer.parseInt(parts[1]);
                        neighborCandidates.add(new int[]{sender, other});

                        System.out.println("Received neighbor candidate: " + sender + ", " + other);

                        // Als we genoeg reacties hebben (optioneel: wachten op totalNodes - 1)
                        if (neighborCandidates.size() >= node.getTotalNodes() - 1) {

                            decideNeighbors(node);
                        }
                    }
                    else {
                        System.err.println("Unknown message format: " + message);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in message: " + message);
            }
        };

        Thread receiverThread = new Thread(new UnicastReceiver(node.getPort(), handler));
        receiverThread.start();
    }

    private void startTcpPingServer(Node node) {
//        new Thread(() -> {
//            try (ServerSocket server = new ServerSocket(node.getPort())) {
//                System.out.printf("✔️ TCP ping server listening on port %d%n", node.getPort());
//                while (true) {
//                    Socket s = server.accept();
//                    s.close();
//                }
//            } catch (IOException ignored) { }
//        }, "TCP-Ping-Server-" + node.getPort()).start();
        System.out.printf("✔️ TCP ping server listening on port %d%n", node.getPort());
    }

    public void decideNeighbors(Node node) {
        int selfHash = HashingFunction.hashNodeName(node.getName());
        int bestPrev = -1;
        int bestNext = -1;
        int minPrevDistance = Integer.MAX_VALUE;
        int minNextDistance = Integer.MAX_VALUE;

        for (int[] pair : neighborCandidates) {
            int candidate = pair[0];
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
//            int nodeHashA = pair[0];
//            int nodeHashB = pair[1];
//            int[] candidates = {nodeHashA, nodeHashB};
//
//            for (int candidate : candidates) {
//                if (candidate == selfHash) continue;
//
//                if (bestNext == -1 || isBetween(selfHash, candidate, bestNext)) {
//                    bestNext = candidate;
//
//                }
//
//                if (bestPrev == -1 || isBetween(bestPrev, candidate, selfHash)) {
//                    bestPrev = candidate;
//                }
//            }
        }

        node.setPreviousID(bestPrev);
        node.setNextID(bestNext);
        System.out.println("Chosen neighbors for " + node.getName() + " → prevID: " + bestPrev + ", nextID: " + bestNext);

        int[] neighborPorts = getUpdatedNeighborsFromNamingServer(node.getPort());
        node.setPreviousPort(neighborPorts[0]);
        node.setNextPort(neighborPorts[1]);

        System.out.printf("   ↳ neighbor ports set: prevPort=%d, nextPort=%d%n",
                neighborPorts[0], neighborPorts[1]);

        //we check wether
        if (!node.getFailureMonitorStarted()) {//&& node.getNextPort() > 0) {  // boolean flag in Node class
            this.startFailureMonitor(node);
        }
    }

//    private boolean isBetween(int start, int x, int end) {
//        if (start < end) {
//            return start < x && x < end;
//        } else {
//            // wrap-around case
//            return start < x || x < end;
//        }
//    }


    //to get and change the neighbors (especially needed for the first node, since it can't otherwise change its neighbors)
    public List<int[]> getNeighborCandidates() {
        return neighborCandidates;
    }



    public void startFailureMonitor(Node node) {
        scheduler.scheduleAtFixedRate(() -> {
            int failedPort = node.getNextPort();
            System.out.printf("…pinging neighbor at port %d…%n", failedPort);
            try (Socket s = new Socket("localhost", failedPort)) {
                s.setSoTimeout(2000);
                s.getOutputStream().write("PING".getBytes());
            } catch (IOException ioe) {
                System.err.println("Failure detected on port " + failedPort);
                int[] nb = getUpdatedNeighborsFromNamingServer(failedPort);
                node.setPreviousPort(nb[0]);
                node.setNextPort(nb[1]);
                System.out.printf("   → new prev=%d, new next=%d%n", nb[0], nb[1]);
                notifyNamingServerToRemoveNode(failedPort);
            }
        }, 5, 5, TimeUnit.SECONDS);
        node.markFailureMonitorStarted();
    }

    public static int[] getUpdatedNeighborsFromNamingServer(int port) {
        try {
            URL url = new URL(NAMING_BASE + "/neighbors?port=" + port);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(2000);
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            NeighborResponse r = new ObjectMapper().readValue(c.getInputStream(), NeighborResponse.class);
            return new int[]{ r.getPrevious(), r.getNext() };
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch neighbors", e);
        }
    }

    public static void notifyNamingServerToRemoveNode(int port) {
        try {
            URL url = new URL(NAMING_BASE + "/nodes?port=" + port);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("DELETE");
            c.setConnectTimeout(2000);
            int code = c.getResponseCode();
            if (code != 200 && code != 204) System.err.println("Warning: delete returned HTTP " + code);
        } catch (IOException e) {
            System.err.println("Error removing node: " + e.getMessage());
        }
    }

    public static void shutdownGracefully(Node node) {
        try {
            // Call Naming Server to get previous/next neighbor info
            String shutdownUrl = NAMING_BASE + "/shutdown?port=" + node.getPort();
            URL url = new URL(shutdownUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 200) {
                // Read neighbors info (prevPort, nextPort)
                Map<String, Integer> neighbors = new ObjectMapper().readValue(con.getInputStream(), Map.class);
                int prevPort = neighbors.get("prevPort");
                int nextPort = neighbors.get("nextPort");

                // Print neighbors' info (for debugging)
                System.out.printf("Updated neighbors after shutdown: prevPort=%d, nextPort=%d%n", prevPort, nextPort);

                // Notify previous node to update its nextID (using unicast)
                notifyPreviousNode(prevPort, nextPort);

                // Notify next node to update its previousID (using unicast)
                notifyNextNode(nextPort, prevPort);

                // Transfer files to the previous node (if any)
                transferFiles(node);

                // After file transfer and neighbor updates, shutdown gracefully
                System.out.println("Node " + node.getName() + " shut down gracefully.");
                System.exit(0);  // Exit JVM after completing the shutdown tasks
            } else {
                System.err.println("Shutdown failed with status: " + con.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);  // Exit JVM with error status
        }
    }

    // Notify the previous node to update its nextID
    private static void notifyPreviousNode(int prevPort, int newNextPort) {
        sendUnicast(prevPort, "UPDATE_NEXT:" + newNextPort);
    }

    // Notify the next node to update its previousID
    private static void notifyNextNode(int nextPort, int newPrevPort) {
        sendUnicast(nextPort, "UPDATE_PREV:" + newPrevPort);
    }

    // Send unicast messages to notify neighbors
    private static void sendUnicast(int targetPort, String message) {
        try {
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, targetPort);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            System.out.println("Unicast sent to " + targetPort + ": " + message);
        } catch (Exception e) {
            System.err.println("Failed to send unicast to port " + targetPort);
        }
    }

    // Transfer replicated files to the previous node
    private static void transferFiles(Node node) {
        List<File> replicatedFiles = node.getReplicatedFileObjects();
        for (File file : replicatedFiles) {
            int prevPort = node.getPreviousPort();  // Get previous node port
            FileSender.sendFile(file, prevPort);    // Transfer each replicated file
        }

        // Notify owners about the shutdown and transfer files if needed
        System.out.println("Transferring files to previous node before shutdown.");
    }


    public static class NeighborResponse {
        private int previous, next;
        public NeighborResponse() {}
        public int getPrevious() { return previous; }
        public int getNext()     { return next;     }
        public void setPrevious(int p) { this.previous = p; }
        public void setNext(int n)     { this.next     = n; }
    }
}
