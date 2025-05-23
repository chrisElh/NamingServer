package Namingserver.namingserver.controller.communication;


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
            System.out.println("FailureListener running on port 8888");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                System.err.println("portnummer dat de failurelistener ontvangt: : " + message);

                if (message.startsWith("FAILURE:")) {
                    int failedPort = Integer.parseInt(message.split(":")[1]);
                    System.out.println("❌ Failure reported for port: " + failedPort);

                    // Call failure handling logic in ServerController
                    controller.handleFailure(failedPort);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
