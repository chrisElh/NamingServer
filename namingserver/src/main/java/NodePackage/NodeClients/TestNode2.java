package NodePackage.NodeClients;


import NodePackage.Node;
import NodePackage.NodeApp;

public class TestNode2 {
    public static void main(String[] args) throws InterruptedException{
        NodeApp app = new NodeApp();
        Node node = app.createAndAnnounceNewNode("TestNode2", 2000);

        Thread.sleep(15000);
//
        NodeApp.shutdownGracefully(node);
    }
}
