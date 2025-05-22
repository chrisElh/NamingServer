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
            ServerSocket sharedSocket = new ServerSocket(unicastPort);
            System.out.println("TCP server socket opened on port " + unicastPort);
            startTcpPingServer(node);
            startUnicastReceiver(node);
            new Thread(new FileReceiver(sharedSocket, dirPathReplica, node)).start();
            MulticastSender.sendMulticast(name, unicastPort, node.getLocalFileNames());
            new Thread(new MulticastReceiver(node, this)).start();
            new Thread(new FileWatcher(node, dirPathLocal + name)).start();

        } catch (Exception e) {
            System.err.println("Error while sending multicast:");
            e.printStackTrace();
        }
        return node;
    }

    private void startUnicastReceiver(Node node) {
        UnicastReceiver.MessageHandler handler = message -> {
            System.out.println("Node " + node.getName() + " received message: " + message);

            try {
                if (message.startsWith("REPLICA:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String filename = parts[1];
                        int targetPort = Integer.parseInt(parts[2]);
                        File file = node.findFileByName(node.getLocalFileObjects(), filename);
                        FileSender.sendFile(file, targetPort);
                    }
                } else {
                    String[] parts = message.trim().split(",");
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
//                    else if (parts.length == 2) {
//                        int sender = Integer.parseInt(parts[0]);
//                        int other = Integer.parseInt(parts[1]);
//                        neighborCandidates.add(new int[]{sender, other});
//
//                        if (neighborCandidates.size() >= node.getTotalNodes() - 1) {
//                            decideNeighbors(node);
//                        }
//                    }
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

    private void decideNeighbors(Node node) {
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
        }

        node.setPreviousID(bestPrev);
        node.setNextID(bestNext);
        System.out.println("Chosen neighbors for " + node.getName() + " → prevID: " + bestPrev + ", nextID: " + bestNext);

        int[] neighborPorts = getUpdatedNeighborsFromNamingServer(node.getPort());
        node.setPreviousPort(neighborPorts[0]);
        node.setNextPort(neighborPorts[1]);

        System.out.printf("   ↳ neighbor ports set: prevPort=%d, nextPort=%d%n",
                neighborPorts[0], neighborPorts[1]);

        startFailureMonitor(node);
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
            String shutdownUrl = NAMING_BASE + "/shutdown?port=" + node.getPort();
            URL url = new URL(shutdownUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() == 200) {
                Map<String, Integer> neighbors = new ObjectMapper().readValue(con.getInputStream(), Map.class);
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

    public static class NeighborResponse {
        private int previous, next;
        public NeighborResponse() {}
        public int getPrevious() { return previous; }
        public int getNext()     { return next;     }
        public void setPrevious(int p) { this.previous = p; }
        public void setNext(int n)     { this.next     = n; }
    }
}
