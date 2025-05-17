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
            // the following runs in the background to detect file addition, here we set up a filewatcher for the given directory
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(directoryPath);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);      //ENTRY_CREATE means: "alert me when a new file appears."
            System.out.println("FileWatcher is now monitoring: " + directoryPath);

            //listening for changes, this waits for the OS to notify you that something changed in the folder.
            while (true) {
                WatchKey key = watchService.take(); // Wait for file changes
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {

                        //handling new file, gets the name of the new file and builds a File object pointing to it
                        String fileName = event.context().toString();
                        File newFile = new File(directoryPath, fileName);

                        // Avoid duplicate entries
                        List<String> currentFiles = node.getLocalFileNames();
                        if (!currentFiles.contains(fileName)) {
                            System.out.println("New file detected: " + fileName);

                            // Register locally in the node object, adds the file to the internal list so it will be treated as part of this node's storage.
                            node.getLocalFileNames().add(fileName);
                            node.getLocalFileObjects().add(newFile);

                            // Notify Naming Server
                            try {
                                String serverUrl = "http://localhost:8080/registerFile?filename=" + fileName + "&nodeName=" + node.getName();
                                URL url = new URL(serverUrl);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setDoOutput(true);


                                //Reading server response, checking for registration of the file
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
                key.reset();        //Required to continue listening after handling an event.


            }

        } catch (Exception e) {
            System.err.println("FileWatcher error:");
            e.printStackTrace();
        }
    }
}
