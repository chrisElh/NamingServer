package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode2 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
      //  app.createAndAnnounceNewNode("TestNode2", 2050);
        URL resource = TestNode1.class.getClassLoader().getResource("files2");
        if (resource == null) {
            System.err.println("Directory not found in resources");
            return;
        }
        File dir = new File(resource.getFile());
        Node node = app.createAndAnnounceNewNode("TestNode1", 2050, dir.getAbsolutePath() );
        System.out.println(node.getLocalFileNames());
    }
}
