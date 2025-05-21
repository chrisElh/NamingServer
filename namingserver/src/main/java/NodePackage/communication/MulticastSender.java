package NodePackage.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

/**
 * This class allows a Node to broadcast its presence to all other nodes
 * using UDP multicast. It sends the node's name, port, and list of local files.
 */
public class MulticastSender {

    private static final String MULTICAST_IP = "230.0.0.0"; // IP address of the multicast group
    private static final int MULTICAST_PORT = 4446;         // Port used by all receivers

    /**
     * Sends a multicast message to announce a node to the network.
     * The message contains the node name, its unicast port, and the list of file names it owns.
     *
     * Format: name,port,file1|file2|file3
     *
     * @param nodeName        the name of the node
     * @param unicastPort     the port on which the node listens for unicast messages
     * @param localFileNames  the list of files owned by the node
     * @throws Exception if multicast sending fails
     */
    public static void sendMulticast(String nodeName, int unicastPort, List<String> localFileNames) throws Exception {
        // Convert the list of file names to a single string using a delimiter (|) that is unlikely in file names
        String files = String.join("|", localFileNames);

        // Construct the full message in the format: name,port,file1|file2|file3
        String message = nodeName + "," + unicastPort + "," + files;

        // Prepare the UDP packet for multicast
        InetAddress group = InetAddress.getByName(MULTICAST_IP);
        byte[] buf = message.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);

        // Create a DatagramSocket and send the packet
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();

        // Debug output
        System.out.println("Multicast sent: " + message);
    }
}
