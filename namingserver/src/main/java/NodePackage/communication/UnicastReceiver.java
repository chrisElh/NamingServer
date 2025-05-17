package NodePackage.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UnicastReceiver implements Runnable {

    private final int port;
    private final MessageHandler handler;

    public UnicastReceiver(int port, MessageHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[256];
            System.out.println("UnicastReceiver listening on UDP port " + port);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                handler.handle(received);
            }
        } catch (Exception e) {
            System.err.println("Error in UnicastReceiver on port " + port);
            e.printStackTrace();
        }
    }

    public interface MessageHandler {
        void handle(String message);
    }
}
