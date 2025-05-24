package NodePackage.communication;

import NodePackage.Agent.SyncAgent;
import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Luistert op een TCP-poort en behandelt inkomende berichten.
 * Stuurt "PING" door naar NodeApp en "STORE_FILE" door naar FileReceiver.
 */
public class TCPMessageHandler implements Runnable {

    private final ServerSocket serverSocket;
    private final Node node;
    private final NodeApp app;
    private final FileReceiver fileReceiver;

    public TCPMessageHandler(ServerSocket serverSocket, Node node, NodeApp app, FileReceiver fileReceiver) {
        this.serverSocket = serverSocket;
        this.node = node;
        this.app = app;
        this.fileReceiver = fileReceiver;
    }

    @Override
    public void run() {
        System.out.println("🟢 TCPMessageHandler listening on port " + node.getPort());

        while (true) {
            try {
                // Wacht op inkomende verbinding
                Socket socket = serverSocket.accept();

                // Verwerk elke verbinding asynchroon
                new Thread(() -> handle(socket)).start();

            } catch (IOException e) {
                System.err.println("❌ TCP accept failed: " + e.getMessage());
            }
        }
    }

    /**
     * Leest de eerste regel van de inkomende verbinding en stuurt door.
     */
    private void handle(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                InputStream input = socket.getInputStream()
        ) {
            String header = reader.readLine();

            if (header == null) {
                System.err.println("❌ Empty TCP header");
                return;
            }

            if (header.startsWith("PING")) {
                System.out.println("✅ Ping received from " + socket.getRemoteSocketAddress());
               // app.decideNeighbors(node); // of andere pinglogica
                return;
            }

            if (header.startsWith("STORE_FILE:")) {
                String fileName = header.substring("STORE_FILE:".length()).trim();
                fileReceiver.handleFile(socket, fileName, input);
                return;
            }

            if (header.startsWith("GET_FILELIST")) {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                List<SyncAgent.FileEntry> entries = node.getSyncAgent().getFileList();

                for (SyncAgent.FileEntry entry : entries) {
                    writer.println(entry.filename + ":" + entry.locked);
                }
                writer.println("END");
                return;
            }


            System.err.println("❌ Unknown TCP header: " + header);

        } catch (IOException e) {
            System.err.println("❌ Error in TCP message handler: " + e.getMessage());
        }
    }
}
