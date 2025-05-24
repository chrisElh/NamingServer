package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode4 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        String dirPathLocal = "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\local";
        String dirPathReplica = "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\replicas";
        Node node = app.createAndAnnounceNewNode("Esa", 3700, dirPathLocal, dirPathReplica);
        // Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\replica");

        System.out.println(node.getLocalFileNames());

    }
}
