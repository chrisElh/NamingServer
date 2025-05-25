package Namingserver.namingserver.controller.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerUnicastSender {

    public static void sendNodeCount(String targetPort, int nodeCount) {
        try {
            String message = String.valueOf(nodeCount);
            InetAddress address = InetAddress.getByName("localhost");
            int port = Integer.parseInt(targetPort);

            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            System.out.println("UDP unicast sent to port " + port + " → value = " + nodeCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendReplicaInstruction(String targetPort, String filename, String destinationPort) {
        try {


            String message = "REPLICA:" + filename + ":" + destinationPort;
            InetAddress address = InetAddress.getByName("localhost");
            int port = Integer.parseInt(targetPort);

            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            System.out.println("Sent replica instruction to port " + port + " → " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendLockInstruction(String targetPort, String filename, boolean lock) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName("localhost");

            String command = (lock ? "LOCK:" : "UNLOCK:") + filename;
            byte[] buffer = command.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    address,
                    Integer.parseInt(targetPort)
            );

            socket.send(packet);
            socket.close();

            System.out.println("📩 Unicast " + (lock ? "LOCK" : "UNLOCK") +
                    " instruction sent to port " + targetPort + " for file " + filename);

        } catch (Exception e) {
            System.err.println("❌ Failed to send lock/unlock instruction: " + e.getMessage());
        }
    }


}