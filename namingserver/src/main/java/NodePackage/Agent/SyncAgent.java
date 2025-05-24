package NodePackage.Agent;

import NodePackage.Node;

import java.io.*;
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

                // Voeg lokale bestanden toe aan fileList als ze nog niet bestaan
                List<String> localFiles = node.getLocalFileNames();
                for (String file : localFiles) {
                    if (fileList.stream().noneMatch(e -> e.filename.equals(file))) {
                        fileList.add(new FileEntry(file, false));
                        System.out.println("📄 Nieuw lokaal bestand toegevoegd aan SyncAgent: " + file);
                    }
                }

                // Synchroniseer met volgende node
                int nextPort = node.getNextPort();
                if (nextPort > 0) {
                    try (
                            Socket socket = new Socket("localhost", nextPort);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    ) {
                        out.println("GET_FILELIST");

                        List<FileEntry> neighborFiles = new ArrayList<>();
                        String line;
                        while ((line = in.readLine()) != null && !line.equals("END")) {
                            String[] parts = line.split(":");
                            if (parts.length != 2) continue;
                            String filename = parts[0];
                            boolean locked = Boolean.parseBoolean(parts[1]);
                            neighborFiles.add(new FileEntry(filename, locked));
                        }

                        // Voeg toe of update bestaande
                        for (FileEntry nf : neighborFiles) {
                            Optional<FileEntry> existing = fileList.stream().filter(e -> e.filename.equals(nf.filename)).findFirst();
                            if (existing.isEmpty()) {
                                fileList.add(nf);
                                System.out.println("➕ Nieuw bestand van neighbor: " + nf.filename + " (locked=" + nf.locked + ")");
                            } else if (existing.get().locked != nf.locked) {
                                existing.get().locked = nf.locked;
                                System.out.println("🔒 Lock-status bijgewerkt: " + nf.filename + " (locked=" + nf.locked + ")");
                            }
                        }

                        // Verwijder bestanden die niet meer bij neighbor zitten
                        fileList.removeIf(entry ->
                                !localFiles.contains(entry.filename) &&
                                        neighborFiles.stream().noneMatch(nf -> nf.filename.equals(entry.filename))
                        );
                    } catch (Exception e) {
                        System.err.println("❌ SyncAgent TCP-connectie met next-node gefaald: " + e.getMessage());
                    }
                }

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }




    public List<FileEntry> getFileList() {
        return new ArrayList<>(fileList);
    }


}