package NodePackage.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastSender {

    private static final String MULTICAST_IP = "230.0.0.0"; // IP address of the multicast group
    private static final int MULTICAST_PORT = 4446;         // Port used by all receivers

    // Sends a multicast message containing the node name, IP address, and unicast port
    public static void sendMulticast(String nodeName, int unicastPort) throws Exception {
        // Construct the message in the format: name,ip,port
        String message = nodeName + "," + unicastPort;

        // Set up the multicast address and convert the message to bytes
        InetAddress group = InetAddress.getByName(MULTICAST_IP);
        byte[] buf = message.getBytes();

        // Create a UDP packet and send it to the multicast group
        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();

        System.out.println("Multicast sent: " + message);
    }
}
