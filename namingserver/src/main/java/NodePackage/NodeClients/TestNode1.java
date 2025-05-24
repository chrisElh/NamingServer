package NodePackage.NodeClients;

import NodePackage.Node;
import NodePackage.NodeApp;

import java.io.File;
import java.net.URL;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();

       // Node node = app.createAndAnnounceNewNode("Christian", 3030, "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\local", "C:\\Users\\chris\\IdeaProjects\\NServer\\namingserver\\src\\main\\resources\\files1\\replicas");
        Node node = app.createAndAnnounceNewNode("Christian", 3030, "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_ReplicaFixed/namingserver/src/main/resources/files1/local", "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_5_shutdown_log/Log_ReplicaFixed/namingserver/src/main/resources/files1/replica");


        System.out.println(node.getLocalFileNames());

    }
}
