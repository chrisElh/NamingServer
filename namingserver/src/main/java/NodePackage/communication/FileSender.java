package NodePackage.communication;

import java.io.*;
import java.net.Socket;

public class FileSender {

    /**
     * Stuurt een bestand via TCP naar een andere node die luistert op een bepaalde poort.
     *
     * @param file        Het File-object dat moet verzonden worden
     * @param targetPort  De poort waarop de ontvangende node luistert
     */
    public static void sendFile(File file, int targetPort) {
        String host = "localhost"; // Veronderstelling: alle nodes draaien lokaal

        try (
                // Open een socketverbinding naar de target node
                Socket socket = new Socket(host, targetPort);

                // Bereid streams voor: OutputStream voor bytes, PrintWriter voor commandolijn
                OutputStream out = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(out, true);

                // Bestandsinvoerstream voor het lezen van de inhoud
                FileInputStream fileInput = new FileInputStream(file)
        ) {
            String fileName = file.getName();

            // 1. Stuur eerst een instructieregel: "STORE_FILE:<bestandsnaam>"
            writer.println("STORE_FILE:" + fileName);

            // 2. Verstuur daarna de inhoud van het bestand in blokken (buffered)
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Bestand '" + fileName + "' verzonden naar poort " + targetPort);

        } catch (IOException e) {
            System.err.println("Fout bij verzenden van bestand: " + e.getMessage());
        }
    }
}
