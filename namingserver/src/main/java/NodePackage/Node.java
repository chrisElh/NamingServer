package NodePackage;

public class Node {

    private String name;
    private String ipAddress;

    // IDs of neighboring nodes in the ring
    private int previousID = -1;
    private int nextID = -1;

    // Optional: total number of nodes in the network (not used in logic)
    private int totalNodes = 0;

    // Constructor: creates a node with a name and IP address
    public Node(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    // Returns the name of the node
    public String getName() {
        return name;
    }

    // Returns the IP address of the node
    public String getIpAddress() {
        return ipAddress;
    }

    // Returns the ID of the previous node in the ring
    public int getPreviousID() {
        return previousID;
    }

    // Sets the ID of the previous node
    public void setPreviousID(int previousID) {
        this.previousID = previousID;
    }

    // Returns the ID of the next node in the ring
    public int getNextID() {
        return nextID;
    }

    // Sets the ID of the next node
    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    // Gets the total number of nodes in the network
    public int getTotalNodes() {
        return totalNodes;
    }

    // Sets the total number of nodes
    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    // Prints the current status of the node
    public void printStatus() {
        System.out.println("Node status:");
        System.out.println("   Name: " + name);
        System.out.println("   IP Address: " + ipAddress);
        System.out.println("   Previous ID: " + previousID);
        System.out.println("   Next ID: " + nextID);
    }
}
