package NodePackage;

import NodePackage.communication.MulticastReceiver;
import NodePackage.communication.MulticastSender;
import NodePackage.communication.UnicastReceiver;
import Functions.HashingFunction;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;


public class NodeApp {
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();



    private final List<int[]> neighborCandidates = new ArrayList<>();

    // Creates a new node, starts listening on its own port, and announces itself via multicast
    public Node createAndAnnounceNewNode(String name, int unicastPort) {
        Node node = new Node(name, unicastPort);

        try {
            // Start the UDP unicast receiver so this node can receive messages (e.g., nodeCount, neighbors)
            startTcpPingServer(node);         // ← minimal addition

            startUnicastReceiver(node);

            // Broadcast this node's presence using multicast
            MulticastSender.sendMulticast(name, unicastPort);

            //new Thread(new MulticastReceiver(node)).start();
            new Thread(new MulticastReceiver(node, this)).start();

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

                } else {
                    System.err.println("Invalid message format: " + message);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in message: " + message);
            }
        };

        Thread receiverThread = new Thread(new UnicastReceiver(node.getPort(), handler));
        receiverThread.start();
    }

    // failure: pinging works with TCP
    private void startTcpPingServer(Node node) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(node.getPort())) {
                System.out.printf("✔️ TCP ping server listening on port %d%n", node.getPort());

                while (true) {
                    Socket s = server.accept();
                    s.close();
                }
            } catch (IOException ignored) { }
        }, "TCP-Ping-Server-" + node.getPort()).start();
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

        // 2) ask Naming Server for the *ports* of those hashes
        //    we call our same endpoint but pass *our* own port
        int[] neighborPorts = getUpdatedNeighborsFromNamingServer(node.getPort());
        node.setPreviousPort(neighborPorts[0]);
        node.setNextPort(neighborPorts[1]);
        System.out.printf("   ↳ neighbor ports set: prevPort=%d, nextPort=%d%n",
                neighborPorts[0], neighborPorts[1]);


        // **start** monitoring now that we know who our neighbors are
        startFailureMonitor(node);
    }

    // Failure code
    /** Periodically “ping” nextID; on IOException treat as failure */
    public void startFailureMonitor(Node node) {
        scheduler.scheduleAtFixedRate(() -> {
            int failedPort = node.getNextPort();
            System.out.printf("…pinging neighbor at port %d…%n", node.getNextPort());
            try (Socket s = new Socket("localhost", failedPort)) {
                s.setSoTimeout(2000);
                // optional heartbeat:
                s.getOutputStream().write("PING".getBytes());
            } catch (IOException ioe) {
                System.err.println("Failure detected on port " + failedPort);

                // 1) fetch replacement neighbors
                int[] nb = getUpdatedNeighborsFromNamingServer(failedPort);
                node.setPreviousPort(nb[0]);
                node.setNextPort(nb[1]);
                System.out.printf("   → new prev=%d, new next=%d%n", nb[0], nb[1]);

                // 2) evict the failed node from Naming Server
                notifyNamingServerToRemoveNode(failedPort);
            }
        }, 5, 5, TimeUnit.SECONDS);
        node.markFailureMonitorStarted();
    }

    private static final String NAMING_BASE = "http://localhost:8080";

    public static int[] getUpdatedNeighborsFromNamingServer(int failedPort) {
        try {
            URL url = new URL(NAMING_BASE + "/neighbors?port=" + failedPort);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();     //open the connectoin, Gets a URLConnection object tied to that URL, cast to the HTTP-specific subclass.
            c.setRequestMethod("GET");
            c.setConnectTimeout(2000);
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());       //Only a 200 OK is acceptable. Anything else (404, 500, etc.) triggers an IOException, which flows to our catch.

            NeighborResponse resp = new ObjectMapper()                      //ObjectMapper (from Jackson) reads the response body and populates a new NeighborResponse instance, using its setters.
                    .readValue(c.getInputStream(), NeighborResponse.class);
            return new int[]{ resp.getPrevious(), resp.getNext() };

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch neighbors", e);
        }
    }

    private static void notifyNamingServerToRemoveNode(int failedPort) {
        try {
            URL url = new URL(NAMING_BASE + "/nodes?port=" + failedPort);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("DELETE");
            c.setConnectTimeout(2000);
            int code = c.getResponseCode();
            if (code!=200 && code!=204) System.err.println("Warning: delete returned HTTP " + code);
        } catch (IOException e) {
            System.err.println("Error removing node: " + e.getMessage());
        }
    }

    // DTO to match the server’s JSON
    public static class NeighborResponse {
        private int previous, next;
        public NeighborResponse() {}
        public int getPrevious() { return previous; }
        public int getNext()     { return next;     }
        public void setPrevious(int p) { this.previous = p; }
        public void setNext(int n)     { this.next     = n; }
    }


    public static void shutdownGracefully(Node node) {
        try {
            String shutdownUrl = "http://localhost:8080/shutdown?port=" + node.getPort();
            URL url = new URL(shutdownUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 200) {
                Map<String, Integer> neighbors = new ObjectMapper()
                        .readValue(con.getInputStream(), Map.class);
                int prevPort = neighbors.get("prevPort");
                int nextPort = neighbors.get("nextPort");

                System.out.printf("🔁 Updated neighbors after shutdown: prevPort=%d, nextPort=%d%n", prevPort, nextPort);
            } else {
                System.err.println("Shutdown failed with status: " + con.getResponseCode());
            }

            System.out.println("✅ Node " + node.getName() + " on port " + node.getPort() + " shut down gracefully.");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


}







//package NodePackage;
//
//import NodePackage.communication.MulticastReceiver;
//import NodePackage.communication.MulticastSender;
//import NodePackage.communication.UnicastReceiver;
//import Functions.HashingFunction;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.io.IOException;
//import java.net.HttpURLConnection;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.net.URL;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class NodeApp {
//    private final ScheduledExecutorService scheduler =
//            Executors.newSingleThreadScheduledExecutor();
//
//    private static final String NAMING_BASE = "http://localhost:8080";
//
//    /**
//     * Create a new node: start TCP ping server, UDP receiver, then multicast join.
//     */
//    public Node createAndAnnounceNewNode(String name, int unicastPort) {
//        Node node = new Node(name, unicastPort);
//
//        // 1) Start the TCP ping server
//        startTcpPingServer(node);
//        // 2) Start the UDP unicast receiver (handles initial bootstrap)
//        startUnicastReceiver(node);
//        // 3) Announce yourself via multicast
//        try {
//            MulticastSender.sendMulticast(name, unicastPort);
//            new Thread(new MulticastReceiver(node)).start();
//        } catch (IOException e) {
//            System.err.println("Error sending multicast: " + e.getMessage());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return node;
//    }
//
//    /**
//     * A minimal TCP server so new Socket(...) connects succeed when alive.
//     */
//    private void startTcpPingServer(Node node) {
//        new Thread(() -> {
//            try (ServerSocket server = new ServerSocket(node.getPort())) {
//                System.out.printf("✔️ TCP ping server listening on port %d%n", node.getPort());
//                while (true) {
//                    Socket client = server.accept();
//                    client.close();
//                }
//            } catch (IOException ignored) {}
//        }, "TCP-Ping-Server-" + node.getPort()).start();
//    }
//
//    /**
//     * Receives bootstrap messages over UDP; starts failure monitor when ring size >1.
//     */
//    private void startUnicastReceiver(Node node) {
//        UnicastReceiver.MessageHandler handler = message -> {
//            System.out.println("Node " + node.getName() + " received: " + message);
//            String[] parts = message.trim().split(",");
//
//            if (parts.length == 1) {
//                // Naming Server’s reply: totalNodes
//                int total = Integer.parseInt(parts[0]);
//                node.setTotalNodes(total);
//                System.out.println("Total nodes in ring: " + total);
//
//                if (total == 1) {
//                    // solo node: point to self
//                    int h = HashingFunction.hashNodeName(node.getName());
//                    node.setPreviousID(h);
//                    node.setNextID(h);
//                    node.setPreviousPort(node.getPort());
//                    node.setNextPort(node.getPort());
//                    System.out.println("Solo node → point to self");
//                } else {
//                    // fetch neighbor ports then start failure monitor
//                    int[] ports = getUpdatedNeighborsFromNamingServer(node.getPort());
//                    node.setPreviousPort(ports[0]);
//                    node.setNextPort(ports[1]);
//                    System.out.printf("Bootstrapped neighbors → prevPort=%d, nextPort=%d%n",
//                            ports[0], ports[1]);
//                    startFailureMonitor(node);
//                }
//            } else {
//                // ignore other messages during bootstrap
//                System.err.println("Ignored message: " + message);
//            }
//        };
//
//        new Thread(new UnicastReceiver(node.getPort(), handler),
//                "UDP-Unicast-Receiver-" + node.getPort())
//                .start();
//    }
//
//    /**
//     * Every 5s, refresh neighbors, attempt TCP connect to nextPort, on IOException recover.
//     */
//    private void startFailureMonitor(Node node) {
//        scheduler.scheduleAtFixedRate(() -> {
//            // refresh neighbors before ping
//            int[] nb = getUpdatedNeighborsFromNamingServer(node.getPort());
//            node.setPreviousPort(nb[0]);
//            node.setNextPort(nb[1]);
//
//            int p = node.getNextPort();
//            System.out.printf("…pinging neighbor at port %d…%n", p);
//            try (Socket s = new Socket("localhost", p)) {
//                s.setSoTimeout(2000);
//            } catch (IOException ex) {
//                System.err.println("⚠️ Failure detected on port " + p);
//                // re-fetch after failure
//                int[] recovery = getUpdatedNeighborsFromNamingServer(node.getPort());
//                node.setPreviousPort(recovery[0]);
//                node.setNextPort(recovery[1]);
//                System.out.printf("   → new prevPort=%d, new nextPort=%d%n",
//                        recovery[0], recovery[1]);
//                evictNode(p);
//            }
//        }, 0, 5, TimeUnit.SECONDS);
//    }
//
//    /** GET /neighbors?port=… → { previous, next } ports JSON */
//    private int[] getUpdatedNeighborsFromNamingServer(int port) {
//        try {
//            URL url = new URL(NAMING_BASE + "/neighbors?port=" + port);
//            HttpURLConnection c = (HttpURLConnection) url.openConnection();
//            c.setRequestMethod("GET");
//            c.setConnectTimeout(2000);
//            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
//            NeighborResponse r = new ObjectMapper()
//                    .readValue(c.getInputStream(), NeighborResponse.class);
//            return new int[]{ r.getPrevious(), r.getNext() };
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to fetch neighbors", e);
//        }
//    }
//
//    /** DELETE /nodes?port=… to remove dead node from Naming Server */
//    private void evictNode(int port) {
//        try {
//            URL url = new URL(NAMING_BASE + "/nodes?port=" + port);
//            HttpURLConnection c = (HttpURLConnection) url.openConnection();
//            c.setRequestMethod("DELETE");
//            c.setConnectTimeout(2000);
//            int code = c.getResponseCode();
//            if (code != 200 && code != 204) System.err.println("Warning: delete returned HTTP " + code);
//        } catch (IOException e) {
//            System.err.println("Error removing node: " + e.getMessage());
//        }
//    }
//
//    /** DTO for JSON { previous, next } */
//    public static class NeighborResponse {
//        private int previous, next;
//        public NeighborResponse() {}
//        public int getPrevious() { return previous; }
//        public int getNext()     { return next;     }
//        public void setPrevious(int p) { this.previous = p; }
//        public void setNext(int n)     { this.next     = n; }
//    }
//}


