
package NodePackage.Agent.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FailureTestSender {

    private static final String NAMING_SERVER_IP = "localhost";
    private static final int NAMING_SERVER_PORT = 8888;  // moet overeenkomen met failureListener poort

    public static void sendFailure(int failedPort) {
        try {
            String message = "FAILURE:" + failedPort;
            byte[] buf = message.getBytes();
            InetAddress address = InetAddress.getByName(NAMING_SERVER_IP);

            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, NAMING_SERVER_PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();

            System.out.println("Test failure report sent for node port: " + failedPort);
        } catch (Exception e) {
            System.err.println("Error sending test failure report:");
            e.printStackTrace();
        }
    }
}

