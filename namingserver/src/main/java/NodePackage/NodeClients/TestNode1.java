package NodePackage.NodeClients;

import NodePackage.NodeApp;

public class TestNode1 {
    public static void main(String[] args) {
        NodeApp app = new NodeApp();
        app.createAndAnnounceNewNode("TestNode1", 4448);
    }
}
