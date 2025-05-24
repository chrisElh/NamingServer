package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode4 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
      //  Node node = app.createAndAnnounceNewNode("Esa", 3700, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\replica");
        // Node node = app.createAndAnnounceNewNode("Esa", 3030, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\replica");
//        Node node = app.createAndAnnounceNewNode("Esa", 3700, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files4\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files4\\replica");

//        Node node = app.createAndAnnounceNewNode("Esa", 3700, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files4\\replicas");
        // Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\edwar\\Universiteit\\6-DIST\\Practicum\\Lab5\\Replication\\namingserver\\src\\main\\resources\\files1\\replica");
//        Node node = app.createAndAnnounceNewNode("Esa", 3700, "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_ReplicaFixed/namingserver/src/main/resources/files4/local", "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_ReplicaFixed/namingserver/src/main/resources/files4/replica");


        Node node = app.createAndAnnounceNewNode("Esa", 3700, "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Testing/namingserver/src/main/resources/files4/local", "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Testing/namingserver/src/main/resources/files4/replica");
        System.out.println(node.getLocalFileNames());

    }
}
