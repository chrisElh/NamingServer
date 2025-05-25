package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) throws InterruptedException{
        NodeApp app = new NodeApp();
        String dirPathLocal = "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_7/GUI_Lab_7/namingserver/src/main/resources/files1/local";
        String dirPathReplica = "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_7/GUI_Lab_7/namingserver/src/main/resources/files1/replica";
        Node node = app.createAndAnnounceNewNode("Christian", 3030, dirPathLocal, dirPathReplica);
       // Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\replica");
       // Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\replicas");
    //    Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\replica");
    //    Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files1\\local", "C:\\3de_jaar\\3_Distributed_Systeem\\Lab5\\namingserver\\src\\main\\resources\\files1\\replicas");

        System.out.println(node.getLocalFileNames());


//        Thread.sleep(45000);
//////
//        NodeApp.shutdownGracefully(node);

    }
}
