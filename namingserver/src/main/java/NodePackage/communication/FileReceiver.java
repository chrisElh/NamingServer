package NodePackage.communication;

import NodePackage.Node;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiver implements Runnable {

    private final int port;
    private final String storageDirectory;
    private final Node node;


    public FileReceiver(int port, String storageDirectory, Node node) {
        this.port = port;
        this.storageDirectory = storageDirectory;
        this.node = node;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("FileReceiver listening on TCP port " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                // Nieuw inkomend bestand verwerken
                new Thread(() -> handleIncomingFile(socket)).start();
            }
        } catch (IOException e) {
            System.err.println(" Error in FileReceiver: " + e.getMessage());
        }
    }

    private void handleIncomingFile(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             InputStream input = socket.getInputStream()) {

            // 1. Lees eerst de opdrachtregel, bv. "STORE_FILE:example.txt"
            String header = reader.readLine();
            if (header == null || !header.startsWith("STORE_FILE:")) {
                System.err.println(" Invalid headerformat");
                return;
            }

            String fileName = header.substring("STORE_FILE:".length());
            File outputFile = new File(storageDirectory, fileName);

            // 2. Open outputstream en schrijf de binnenkomende data byte-per-byte
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }


            //We add the file to the arrays of the object
            node.addReplicatedFile(outputFile);

//            System.out.println("file added: " + outputFile.getAbsolutePath());
            System.out.println("File received in :" + outputFile);



        } catch (IOException e) {
            System.err.println("Error at the receiver file: " + e.getMessage());
        }
    }
}
