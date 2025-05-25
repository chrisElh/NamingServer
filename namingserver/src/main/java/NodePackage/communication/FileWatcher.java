package NodePackage.communication;

import NodePackage.Agent.SyncAgent;
import NodePackage.Node;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FileWatcher implements Runnable {

    private final Node node;
    private final String directoryPath;
    private final SyncAgent syncAgent;
    private final ConcurrentHashMap<String, Boolean> lockState = new ConcurrentHashMap<>();

    public FileWatcher(Node node, String directoryPath, SyncAgent syncAgent) {
        this.node = node;
        this.directoryPath = directoryPath;
        this.syncAgent = syncAgent;
    }

    @Override
    public void run() {
        try {
            // Zorg dat de directory bestaat
            Path path = Paths.get(directoryPath);
            Files.createDirectories(path);

            // FileWatcher opzetten
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            System.out.println("📁 FileWatcher is now monitoring: " + directoryPath);

            // Polling-thread voor file locks
            new Thread(() -> {
                while (true) {
                    try {
                        for (String fileName : node.getLocalFileNames()) {
                            File file = new File(directoryPath, fileName);
                            boolean isLocked = isFileLocked(file);

                            if (isLocked && !lockState.getOrDefault(fileName, false)) {
                                System.out.println("🔒 File locked (extern geopend): " + fileName);
                                syncAgent.lockFile(fileName);
                                lockState.put(fileName, true);
                            } else if (!isLocked && lockState.getOrDefault(fileName, false)) {
                                System.out.println("🔓 File unlocked (extern gesloten): " + fileName);
                                syncAgent.unlockFile(fileName);
                                lockState.put(fileName, false);
                            }
                        }
                        Thread.sleep(500); // 1 seconde pauze
                    } catch (Exception e) {
                        System.err.println("❌ Lock polling error:");
                        e.printStackTrace();
                    }
                }
            }).start();

            // Event-based bestandsdetectie
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    String fileName = event.context().toString();
                    File affectedFile = new File(directoryPath, fileName);

                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        List<String> currentFiles = node.getLocalFileNames();
                        if (!currentFiles.contains(fileName)) {
                            System.out.println("📄 New file detected: " + fileName);
                            node.getLocalFileNames().add(fileName);
                            node.getLocalFileObjects().add(affectedFile);
                            sendHttpPost("http://localhost:8080/registerFile?filename=" + fileName + "&nodeName=" + node.getName());
                        }
                    }

                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        System.out.println("✏️ File modified → sending lock: " + fileName);
                        syncAgent.lockFile(fileName);
                    }

                    if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.println("🗑️ File deleted → sending unlock: " + fileName);
                        syncAgent.unlockFile(fileName);
                    }
                }
                key.reset();
            }

        } catch (Exception e) {
            System.err.println("❌ FileWatcher error:");
            e.printStackTrace();
        }
    }

    private void sendHttpPost(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✅ POST success: " + urlString);
            } else {
                System.err.println("❌ POST failed: HTTP " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("❌ HTTP POST error: " + urlString);
            e.printStackTrace();
        }
    }

    private boolean isFileLocked(File file) {
        try (FileChannel channel = new RandomAccessFile(file, "rw").getChannel()) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                return true; // al gelocked
            }
            lock.release();
            return false;
        } catch (Exception e) {
            return true; // fout bij openen = waarschijnlijk gelocked
        }
    }
}
