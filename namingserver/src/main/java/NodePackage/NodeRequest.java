package NodePackage;

public class NodeRequest {
    private int port;
    private String name;
    private String localPath;
    private String replicaPath;

    public NodeRequest() {}

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getReplicaPath() {
        return replicaPath;
    }

    public void setReplicaPath(String replicaPath) {
        this.replicaPath = replicaPath;
    }
}
