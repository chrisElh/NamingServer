package Namingserver.namingserver.controller.communication;

import NodePackage.Node;
import Namingserver.namingserver.controller.ServerController;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.List;

public class MulticastReceiver implements Runnable {

    private static final String MULTICAST_IP = "230.0.0.0";
    private static final int MULTICAST_PORT = 4446;

    private final ServerController controller;

    public MulticastReceiver(ServerController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            socket.joinGroup(group);

            System.out.println("MulticastReceiver listening on " + MULTICAST_IP + ":" + MULTICAST_PORT);

            while (true) {
                byte[] buf = new byte[512]; // increase buffer size to handle long file lists
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("Received multicast: " + received);

                // Expected format: name,port,file1|file2|file3
                String[] parts = received.split(",", 3); // limit = 3 to avoid breaking file list

                if (parts.length >= 2) {
                    String name = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    // Parse file list if present
                    List<String> localFiles = List.of();
                    if (parts.length == 3 && !parts[2].isEmpty()) {
                        localFiles = Arrays.asList(parts[2].split("\\|"));
                    }

                    Node node = new Node(name, port);
                    controller.addNodeFromMulticast(node, localFiles);

                    int nodeCount = controller.getNodeCount();
                    ServerUnicastSender.sendNodeCount(String.valueOf(port), nodeCount);
                } else {
                    System.err.println("Invalid multicast format: " + received);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
