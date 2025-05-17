package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
//        URL resource = TestNode1.class.getClassLoader().getResource("files1");
//        if (resource == null) {
//            System.err.println("Directory not found in resources");
//            return;
//        }
//        File dir = new File(resource.getFile());
        Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\replicas");
        System.out.println(node.getLocalFileNames());


//        node.loadLocalFilesFromDirectory(dir.getAbsolutePath());



        //node.loadLocalFilesFromDirectory("src/main/data/files");


    }
}
