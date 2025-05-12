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
}