package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        Node node = app.createAndAnnounceNewNode("TestNode1", 3030, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources" );
        System.out.println(node.getLocalFileNames());
//        URL resource = TestNode1.class.getClassLoader().getResource("files");
//        if (resource == null) {
//            System.err.println("Directory not found in resources");
//            return;
//        }
//        File dir = new File(resource.getFile());
//
//        node.loadLocalFilesFromDirectory(dir.getAbsolutePath());



        //node.loadLocalFilesFromDirectory("src/main/data/files");


    }
}
