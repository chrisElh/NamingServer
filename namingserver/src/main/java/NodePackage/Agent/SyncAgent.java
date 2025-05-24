package NodePackage.Agent;

import NodePackage.Node;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * SyncAgent houdt de lokale lijst van bestanden bij en synchroniseert deze periodiek
 * met de volgende node in de ring. Per bestand wordt ownership en lockingstatus opgeslagen.
 */
public class SyncAgent implements Runnable, Serializable {

    private final Node node;
    private final List<FileEntry> fileList = new ArrayList<>();

    public SyncAgent(Node node) {
        this.node = node;
    }

    /**
     * FileEntry bevat metadata per bestand: naam, lock-status en eigenaar.
     */
    public static class FileEntry {
        public String filename;
        public boolean locked;
        public String owner;

        public FileEntry(String filename, boolean locked, String owner) {
            this.filename = filename;
            this.locked = locked;
            this.owner = owner;
        }
    }

    /**
     * Periodieke synchronisatie van de fileList met eigen lokale bestanden en neighbor-bestanden.
     */
    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("🔁 SyncAgent uitvoer op node " + node.getName());

                // Voeg nieuwe lokale bestanden toe aan de lijst met node als eigenaar
                List<String> localFiles = node.getLocalFileNames();
                for (String file : localFiles) {
                    if (fileList.stream().noneMatch(e -> e.filename.equals(file))) {
                        fileList.add(new FileEntry(file, false, node.getName()));
                        System.out.println("📄 Nieuw lokaal bestand toegevoegd aan SyncAgent: " + file);
                    }
                }

                // Synchroniseer met de volgende node in de ring (alleen indien nextPort bekend)
                int nextPort = node.getNextPort();
                if (nextPort > 0) {
                    try (
                            Socket socket = new Socket("localhost", nextPort);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                    ) {
                        // Vraag bestandslijst op
                        out.println("GET_FILELIST");

                        List<FileEntry> neighborFiles = new ArrayList<>();
                        String line;
                        while ((line = in.readLine()) != null && !line.equals("END")) {
                            String[] parts = line.split(":");
                            if (parts.length != 3) continue;
                            String filename = parts[0];
                            boolean locked = Boolean.parseBoolean(parts[1]);
                            String owner = parts[2];
                            neighborFiles.add(new FileEntry(filename, locked, owner));
                        }

                        // Voeg nieuwe neighbor-bestanden toe of werk bestaande bij
                        for (FileEntry nf : neighborFiles) {
                            Optional<FileEntry> existing = fileList.stream()
                                    .filter(e -> e.filename.equals(nf.filename))
                                    .findFirst();

                            if (existing.isEmpty()) {
                                fileList.add(nf);
                                System.out.println("➕ Nieuw bestand van neighbor: " + nf.filename +
                                        " (locked=" + nf.locked + ", owner=" + nf.owner + ")");
                            } else {
                                // Update alleen lock-status indien gewijzigd
                                FileEntry current = existing.get();
                                if (current.locked != nf.locked) {
                                    current.locked = nf.locked;
                                    System.out.println("🔒 Lock-status bijgewerkt: " + nf.filename +
                                            " (locked=" + nf.locked + ")");
                                }
                            }
                        }

                        // Verwijder verouderde bestanden (niet lokaal én niet bij neighbor)
                        fileList.removeIf(entry ->
                                !localFiles.contains(entry.filename) &&
                                        neighborFiles.stream().noneMatch(nf -> nf.filename.equals(entry.filename))
                        );

                    } catch (Exception e) {
                        System.err.println("❌ SyncAgent TCP-connectie met next-node gefaald: " + e.getMessage());
                    }
                }

                // Wacht 5 seconden voor de volgende synchronisatie
                Thread.sleep(5000);

            } catch (InterruptedException e) {
                // Thread wordt netjes afgesloten bij interrupt
                break;
            }
        }
    }


    /**
     * Probeert het opgegeven bestand te locken.
     * Alleen de owner mag dit doen. Returnt true als gelukt.
     */
    public boolean lockFile(String filename) {
        Optional<FileEntry> entryOpt = fileList.stream()
                .filter(e -> e.filename.equals(filename))
                .findFirst();

        if (entryOpt.isPresent()) {
            FileEntry entry = entryOpt.get();
            if (entry.owner.equals(node.getName())) {
                entry.locked = true;
                System.out.println("🔒 Bestand gelockt: " + filename);
                return true;
            } else {
                System.out.println("🚫 Kan niet locken, geen owner: " + filename);
            }
        } else {
            System.out.println("❌ Bestand niet gevonden voor locking: " + filename);
        }
        return false;
    }

    /**
     * Probeert het opgegeven bestand te unlocken.
     * Alleen de owner mag dit doen. Returnt true als gelukt.
     */
    public boolean unlockFile(String filename) {
        Optional<FileEntry> entryOpt = fileList.stream()
                .filter(e -> e.filename.equals(filename))
                .findFirst();

        if (entryOpt.isPresent()) {
            FileEntry entry = entryOpt.get();
            if (entry.owner.equals(node.getName())) {
                entry.locked = false;
                System.out.println("🔓 Bestand ge-unlockt: " + filename);
                return true;
            } else {
                System.out.println("🚫 Kan niet unlocken, geen owner: " + filename);
            }
        } else {
            System.out.println("❌ Bestand niet gevonden voor unlocking: " + filename);
        }
        return false;
    }



    /**
     * Geef de huidige lijst met FileEntry terug (kopie).
     */
    public List<FileEntry> getFileList() {
        return new ArrayList<>(fileList);
    }
}
