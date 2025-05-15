package NodePackage.communication;
import java.io.*;
import java.net.Socket;


public class FileSender {

    /**
     * Stuurt een bestand via TCP naar een andere node die luistert op een bepaalde poort.
     *
     * @param filePath     pad naar het bestand dat moet verzonden worden (bv. "./data/example.txt")
     * @param targetPort   poort van de ontvangende node
     */
    public static void sendFile(String filePath, int targetPort) {
        String host = "localhost"; // Als alle nodes lokaal draaien

        try (Socket socket = new Socket(host, targetPort);
             OutputStream out = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true);
             FileInputStream fileInput = new FileInputStream(filePath)) {

            File file = new File(filePath);
            String fileName = file.getName();

            // 1. Stuur eerst de opdrachtregel, bv. STORE_FILE:<filename>
            writer.println("STORE_FILE:" + fileName);

            // 2. Stuur daarna de inhoud van het bestand byte-per-byte
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Bestand '" + fileName + "' verzonden naar poort " + targetPort);

        } catch (IOException e) {
            System.err.println(" Fout bij verzenden van bestand: " + e.getMessage());
        }
    }
}