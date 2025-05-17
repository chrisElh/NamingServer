package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        Node node = app.createAndAnnounceNewNode("TestNode1", 6666);
        node.loadLocalFilesFromDirectory("./data/files");
//        String absPath = new File("src/main/data/files").getAbsolutePath();
//        node.loadLocalFilesFromDirectory(absPath);


    }
}
