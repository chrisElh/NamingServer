package NodePackage.NodeClients;


import NodePackage.Node;
import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode2 {
    public static void main(String[] args) throws InterruptedException{
        NodeApp app = new NodeApp();
      //  app.createAndAnnounceNewNode("TestNode2", 2050);
//        URL resource = TestNode1.class.getClassLoader().getResource("files2");
//        if (resource == null) {
//            System.err.println("Directory not found in resources");
//            return;
//        }
//        File dir = new File(resource.getFile());

      //  Node node = app.createAndAnnounceNewNode("Setare", 2050, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files2\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files2\\replica");
       // Node node = app.createAndAnnounceNewNode("Setare", 9060, "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_NamingServer/namingserver/src/main/resources/files2/local", "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_NamingServer/namingserver/src/main/resources/files2/replica");
        Node node = app.createAndAnnounceNewNode("Setare", 9060, "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files2\\local", "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files2\\replicas");

        System.out.println(node.getLocalFileNames());


        Thread.sleep(15000);
////
        NodeApp.shutdownGracefully(node);

    }
}
