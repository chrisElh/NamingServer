package NodePackage;

import NodePackage.communication.MulticastSender;
import NodePackage.communication.UnicastReceiver;

public class NodeApp {

    // Creates a new node, starts listening on its own port, and announces itself via multicast
    public Node createAndAnnounceNewNode(String name, int unicastPort) {
        Node node = new Node(name, unicastPort);

        try {
            // Start the UDP unicast receiver so this node can receive messages (e.g., nodeCount, neighbors)
            startUnicastReceiver(node);

            // Broadcast this node's presence using multicast
            MulticastSender.sendMulticast(name, unicastPort);
        } catch (Exception e) {
            System.err.println("Error while sending multicast:");
            e.printStackTrace();
        }

        return node;
    }

    // Starts a thread to listen for UDP unicast messages on the node's port
    private void startUnicastReceiver(Node node) {
        UnicastReceiver.MessageHandler handler = message -> {
            System.out.println("Node " + node.getName() + " received message: " + message);

            try {
                int total = Integer.parseInt(message.trim());
                node.setTotalNodes(total);
                System.out.println("Updated totalNodes to: " + total);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in message: " + message);
            }
        };

        Thread receiverThread = new Thread(new UnicastReceiver(node.getPort(), handler));
        receiverThread.start();
    }

}
