package NodePackage.communication;

import NodePackage.Node;

import java.io.*;
import java.net.Socket;

/**
 * Verwerkt bestanden die via TCP worden ontvangen.
 * Dit component doet geen accept(), maar enkel verwerking.
 */
public class FileReceiver {

    private final String storageDirectory;
    private final Node node;

    public FileReceiver(String storageDirectory, Node node) {
        this.storageDirectory = storageDirectory;
        this.node = node;
    }

    /**
     * Ontvangt en slaat het bestand op in de replicamap.
     *
     * @param socket    de actieve socket met de afzender
     * @param fileName  bestandsnaam uit de header
     * @param input     inputstream van de socket
     */
    public void handleFile(Socket socket, String fileName, InputStream input) {
        try {
            File outputFile = new File(storageDirectory, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }

            node.addReplicatedFile(outputFile);
            System.out.println("📁 File stored: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("❌ Error saving file: " + e.getMessage());
        }
    }
}
