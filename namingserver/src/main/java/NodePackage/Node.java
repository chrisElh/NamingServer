package NodePackage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Node {

    private String name;
    private int port; // NEW: unicast port

    // IDs of neighboring nodes in the ring
    private int previousID = -1;
    private int nextID = -1;

    // Optional: total number of nodes in the network (not used in logic)
    private int totalNodes = 0;


    private List<File> localFileObjects = new ArrayList<>();
    private List<String> localFileNames = new ArrayList<>();
    private List<File> replicatedFileObjects = new ArrayList<>();
    private List<String> replicatedFileNames = new ArrayList<>();


    // Constructor: creates a node with a name, IP address, and port
    public Node(String name, int port) {
        this.name = name;
        this.port = port;
    }

    // Getters
    public String getName() {
        return name;
    }


    public int getPort() {
        return port;
    }

    // Setters
    public void setPort(int port) {
        this.port = port;
    }

    public int getPreviousID() {
        return previousID;
    }

    public void setPreviousID(int previousID) {
        this.previousID = previousID;
    }

    public int getNextID() {
        return nextID;
    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    // Print current status of the node
    public void printStatus() {
        System.out.println("Node status:");
        System.out.println("   Name: " + name);
        System.out.println("   Port: " + port); // NEW
        System.out.println("   Previous ID: " + previousID);
        System.out.println("   Next ID: " + nextID);
    }

    // ----------------------------
// Local file methods
// ----------------------------

    /**
     * Scans a specified directory and loads all regular files into:
     *  - localFileObjects: to keep full access to the File (for reading/sending)
     *  - localFileNames: to register the file names with the Naming Server
     *
     * @param directoryPath The folder path containing local files (e.g. "./files")
     */
    public void loadLocalFilesFromDirectory(String directoryPath) {
        // Create a File object representing the directory
        File dir = new File(directoryPath);

        // Check if the path exists and is a valid directory
        if (dir.exists() && dir.isDirectory()) {

            // List all files and subfolders inside the directory
            File[] files = dir.listFiles();

            // Make sure files were successfully listed (not null)
            if (files != null) {
                // Clear any previously stored files/names
                localFileObjects.clear();
                localFileNames.clear();

                // Loop over all items in the folder
                for (File file : files) {
                    // Only consider actual files (ignore subdirectories)
                    if (file.isFile()) {
                        // Store the file object for internal use (e.g. sending)
                        localFileObjects.add(file);

                        // Store just the file name for server registration
                        localFileNames.add(file.getName());
                    }
                }
            }

        } else {
            // If the folder does not exist or is invalid, show an error
            System.err.println("Invalid file directory: " + directoryPath);
        }
    }

    // Return only the names of the local files (for communication with server)
    public List<String> getLocalFileNames() {
        return localFileNames;
    }

    // Return actual File objects
    public List<File> getLocalFileObjects() {
        return localFileObjects;
    }

    // Optional setter if needed manually
    public void setLocalFileNames(List<String> names) {
        this.localFileNames = names;
    }

    // Other existing methods (name, port, etc.) remain unchanged



}
