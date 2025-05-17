package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        Node node = app.createAndAnnounceNewNode("TestNode1", 3030, "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files" );
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
