package NodePackage.Agent;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FailureAgent {

    private static final String NAMING_SERVER_IP = "localhost";
    private static final int NAMING_SERVER_PORT = 8888;  // de poort waarop de FailureListener luistert

    /**
     * Stuurt een UDP-melding naar de Naming Server dat een bepaalde node gefaald is.
     *
     * @param failedPort de poort van de node die niet meer reageert
     */
    public static void reportFailure(int failedPort) {
        try {
            System.err.println("gefaalde portnummer dat de failureagent stuurt: " + failedPort);
            String message = "FAILURE:" + failedPort;
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(NAMING_SERVER_IP);

            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, NAMING_SERVER_PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            System.out.println("🚨 Failure reported for node on port " + failedPort);
        } catch (Exception e) {
            System.err.println("❌ Error while reporting failure:");
            e.printStackTrace();
        }
    }
}
