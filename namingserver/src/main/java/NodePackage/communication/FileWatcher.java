package NodePackage.communication;

import NodePackage.Node;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

public class FileWatcher implements Runnable {

    private final Node node;
    private final String directoryPath;

    public FileWatcher(Node node, String directoryPath) {
        this.node = node;
        this.directoryPath = directoryPath;
    }

    @Override
    public void run() {
        try {
            // Zorg dat de directory bestaat (maak aan indien nodig)
            Path path = Paths.get(directoryPath);
            Files.createDirectories(path);

            // FileWatcher opzetten om nieuwe bestanden te detecteren
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            System.out.println("FileWatcher is now monitoring: " + directoryPath);

            // Wachten op bestandswijzigingen
            while (true) {
                WatchKey key = watchService.take(); // Blokkeert tot er iets gebeurt
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {

                        // Nieuw bestand gedetecteerd
                        String fileName = event.context().toString();
                        File newFile = new File(directoryPath, fileName);

                        // Voorkom dubbele registratie
                        List<String> currentFiles = node.getLocalFileNames();
                        if (!currentFiles.contains(fileName)) {
                            System.out.println("New file detected: " + fileName);

                            // Voeg toe aan node-lijsten
                            node.getLocalFileNames().add(fileName);
                            node.getLocalFileObjects().add(newFile);

                            // Registreer het bestand bij de Naming Server
                            try {
                                String serverUrl = "http://localhost:8080/registerFile?filename=" + fileName + "&nodeName=" + node.getName();
                                URL url = new URL(serverUrl);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setDoOutput(true);

                                int responseCode = connection.getResponseCode();
                                if (responseCode == 200) {
                                    System.out.println("File registered with Naming Server: " + fileName);
                                } else {
                                    System.err.println("Failed to register file: HTTP " + responseCode);
                                }

                            } catch (Exception e) {
                                System.err.println("Error contacting Naming Server:");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                key.reset(); // Belangrijk om verder te kunnen luisteren
            }

        } catch (Exception e) {
            System.err.println("FileWatcher error:");
            e.printStackTrace();
        }
    }
}
