package NodePackage.communication;

import Functions.HashingFunction;
import NodePackage.Node;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Deze klasse laat een Node luisteren naar multicast-berichten.
 * Elke Node die deze klasse opstart, zal nieuwe nodes detecteren in het netwerk
 * en eventueel zijn eigen buren (previousID, nextID) bijwerken.
 */
public class MulticastReceiver implements Runnable {

    private final Node node; // De lokale node die dit ontvangt
    private static final String MULTICAST_IP = "230.0.0.0"; // Multicastgroep
    private static final int MULTICAST_PORT = 4446;         // Multicastpoort

    public MulticastReceiver(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {

            // Sluit aan op de multicastgroep
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            socket.joinGroup(group);
            System.out.println("MulticastReceiver listening on " + MULTICAST_IP + ":" + MULTICAST_PORT);

            byte[] buffer = new byte[256];

            while (true) {
                // Ontvang een multicastpakket
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Extract en verwerk het bericht (bijv. "node2,5002")
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] parts = msg.split(",");

                // Als het bericht niet in correct formaat is, negeren
                if (parts.length != 2) continue;

                String newName = parts[0];              // Naam van de nieuwe node
                int newPort = Integer.parseInt(parts[1]); // Poort van de nieuwe node

                // Bereken hashes
                int newHash = HashingFunction.hashNodeName(newName);          // Hash van nieuwe node
                int currentID = HashingFunction.hashNodeName(node.getName()); // Hash van deze node zelf
                int previousID = node.getPreviousID();
                int nextID = node.getNextID();

                boolean updated = false;

                /**
                 * Bepaal of de nieuwe node tussen deze node en zijn nextID valt
                 * → Dan moet deze node zijn nextID bijwerken naar de nieuwe node
                 */
                if (currentID < newHash && (nextID == -1 || newHash < nextID)) {
                    node.setNextID(newHash);
                    sendResponse(newPort, currentID + "," + nextID); // currentID = deze node, nextID = oud nextID
                    updated = true;
                }

                /**
                 * Bepaal of de nieuwe node tussen previousID en deze node zit
                 * → Dan moet deze node zijn previousID bijwerken naar de nieuwe node
                 */
                if (previousID < newHash && newHash < currentID) {
                    node.setPreviousID(newHash);
                    sendResponse(newPort, currentID + "," + previousID); // currentID = deze node, previousID = oud previousID
                    updated = true;
                }

                if (updated) {
                    System.out.println("Node " + node.getName() + " updated neighbors due to " + newName);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in MulticastReceiver:");
            e.printStackTrace();
        }
    }

    /**
     * Verzendt een unicastantwoord naar de opgegeven poort van de nieuwe node.
     * De inhoud bevat de hash van deze node + de vorige/volgende ID.
     */
    private void sendResponse(int targetPort, String message) {
        try {
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName("localhost");

            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, targetPort);
            new java.net.DatagramSocket().send(packet);

            System.out.println("Sent unicast response to " + targetPort + ": " + message);
        } catch (Exception e) {
            System.err.println("Failed to send response to port " + targetPort);
        }
    }
}
