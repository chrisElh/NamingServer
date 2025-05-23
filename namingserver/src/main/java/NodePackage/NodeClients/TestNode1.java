package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files1\\local", "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files1\\replica");
        System.out.println(node.getLocalFileNames());

    }
}
