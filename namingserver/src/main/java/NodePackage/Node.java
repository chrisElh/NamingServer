package NodePackage;

public class Node {

    private String name;
    private int port; // NEW: unicast port

    // IDs of neighboring nodes in the ring
    private int previousID = -1;
    private int nextID = -1;

    // Optional: total number of nodes in the network (not used in logic)
    private int totalNodes = 0;

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
}
