package Namingserver.namingserver.controller.communication;

import NodePackage.Node;
import Namingserver.namingserver.controller.ServerController;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received multicast: " + received);

                // Verwacht formaat: name,port
                String[] parts = received.split(",");
                if (parts.length == 2) {
                    String name = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    Node node = new Node(name, port);
                    controller.addNodeFromMulticast(node);
                    int nodeCount = controller.getNodeCount(); // Je voegt zelf deze getter toe
                    ServerUnicastSender.sendNodeCount(String.valueOf(port), nodeCount);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
