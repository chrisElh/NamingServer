package NodePackage.Agent;

import NodePackage.Node;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;

/**
 * SyncAgent houdt de lokale lijst bij van alle bestanden en synchroniseert deze met de volgende node.
 */
public class SyncAgent implements Runnable, Serializable {

    private final Node node;
    private final List<FileEntry> fileList = new ArrayList<>();

    public SyncAgent(Node node) {
        this.node = node;
    }

    public static class FileEntry {
        public String filename;
        public boolean locked;

        public FileEntry(String filename, boolean locked) {
            this.filename = filename;
            this.locked = locked;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("🔁 SyncAgent uitvoer op node " + node.getName());

                // voeg lokale bestanden toe
                List<String> localFiles = node.getLocalFileNames();
                for (String file : localFiles) {
                    if (fileList.stream().noneMatch(e -> e.filename.equals(file))) {
                        fileList.add(new FileEntry(file, false));
                        System.out.println("📄 Nieuw lokaal bestand toegevoegd aan SyncAgent: " + file);
                    }
                }

                // sync met volgende node (alleen als nextPort > 0)
                int nextPort = node.getNextPort();
                if (nextPort > 0) {
                    try (
                            Socket socket = new Socket("localhost", nextPort);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    ) {
                        out.println("GET_FILELIST");
                        String line;
                        while ((line = in.readLine()) != null && !line.equals("END")) {
                            String[] parts = line.split(":");
                            if (parts.length != 2) continue;

                            String filename = parts[0];
                            boolean locked = Boolean.parseBoolean(parts[1]);

                            boolean exists = fileList.stream().anyMatch(e -> e.filename.equals(filename));
                            if (!exists) {
                                fileList.add(new FileEntry(filename, locked));
                                System.out.println("➕ Gesynchroniseerd bestand van next: " + filename + " (locked=" + locked + ")");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("❌ SyncAgent TCP-connectie met next-node gefaald: " + e.getMessage());
                    }
                }

                // wacht 5 seconden voor volgende sync
                Thread.sleep(5000);

            } catch (InterruptedException e) {
                break; // stopt de thread netjes bij interrupt
            }
        }
    }


    public List<FileEntry> getFileList() {
        return new ArrayList<>(fileList);
    }


}