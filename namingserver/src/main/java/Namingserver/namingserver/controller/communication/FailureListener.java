package Namingserver.namingserver.controller.communication;

import Namingserver.namingserver.controller.Agent.FailureAgent;
import Namingserver.namingserver.controller.ServerController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class FailureListener implements Runnable {

    private final ServerController controller;

    public FailureListener(ServerController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(8888)) {
            byte[] buffer = new byte[256];
            System.err.println("FailureListener running on port 8888");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                System.out.println("FailureListener received message: " + message);

                if (message.startsWith("FAILURE:")) {
                    int failedPort = Integer.parseInt(message.split(":")[1]);
                    System.err.println("FailureListener detected failure on port: " + failedPort);

                    // Start FailureAgent async in nieuwe thread
                    new Thread(new FailureAgent(controller, failedPort)).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
