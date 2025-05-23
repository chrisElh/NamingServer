package NodePackage.NodeClients;


import NodePackage.Node;
import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode3 {
    public static void main(String[] args) throws InterruptedException{
        NodeApp app = new NodeApp();
        //  app.createAndAnnounceNewNode("TestNode2", 2050);
//        URL resource = TestNode1.class.getClassLoader().getResource("files2");
//        if (resource == null) {
//            System.err.println("Directory not found in resources");
//            return;
//        }
//        File dir = new File(resource.getFile());
        Node node = app.createAndAnnounceNewNode("Edward", 2570, "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files3\\locale", "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files3\\replica");
        System.out.println(node.getLocalFileNames());
//        Node node = app.createAndAnnounceNewNode("TestNode2", 2000);

//        Thread.sleep(15000);
//
//        NodeApp.shutdownGracefully(node);
    }
}
