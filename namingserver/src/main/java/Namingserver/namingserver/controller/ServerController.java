package Namingserver.namingserver.controller;

import NodePackage.Node;
import Functions.HashingFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
public class ServerController {

    // Max and min values used for boundary checks (currently unused)
    private static final int MAX = Integer.MAX_VALUE;
    private static final int MIN = -Integer.MAX_VALUE;

    // Maps hashed node IDs to IP addresses
    private TreeMap<Integer, Integer> nodeMap = new TreeMap<>();

    // Maps IP addresses to node names (to reconstruct full Node objects)
    private Map<String, String> ipToName = new HashMap<>();

    // Maps file names to node hashes (ownership)
    private Map<String, Integer> fileToNodeMap = new HashMap<>();

    // Stores the actual file names per node
    private Map<Integer, List<String>> localFiles = new HashMap<>();

    // Stores replicated files per node
    private Map<Integer, List<String>> replicas = new HashMap<>();

    // Writes the current state of the node map to a file on disk
    private void saveNodeMapToDisk() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("nodeMap.json"), nodeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    // Returns the previous node (circular)
//    @PostMapping("/getPrevious")
//    public Node getPrevious(@RequestBody Node node) {
//        int hash = HashingFunction.hashNodeName(node.getName());
//        Integer prevHash = nodeMap.lowerKey(hash);
//
//        if (prevHash == null) {
//            prevHash = nodeMap.lastKey();
//        }
//
//        String prevIp = nodeMap.get(prevHash);
//        String name = getNodeNameFromIp(prevIp);
//        int port = node.getPort();
//
//        return new Node(prevIp, name, port);
//    }

    // Returns the next node (circular)
//    @PostMapping("/getNext")
//    public Node getNext(@RequestBody Node node) {
//        int hash = HashingFunction.hashNodeName(node.getName());
//        Integer nextHash = nodeMap.higherKey(hash);
//
//        if (nextHash == null) {
//            nextHash = nodeMap.firstKey();
//        }
//
//        String nextIp = nodeMap.get(nextHash);
//        String name = getNodeNameFromIp(nextIp);
//
//        return new Node(nextIp, name);
//    }

    // Helper method to retrieve a node name based on its IP
//    private String getNodeNameFromIp(String ip) {
//        return ipToName.getOrDefault(ip, "unknown");
//    }

    //Functie om node toe te voegen aan map vanuit multicast
    public String addNodeFromMulticast(Node node) {
        int hash = HashingFunction.hashNodeName(node.getName());
        nodeMap.put(hash, node.getPort());
        saveNodeMapToDisk();

        return "Node added: " + node.getName() +
                " (hash: " + hash +
                ", Port: " + node.getPort() + ")";

    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    @PostMapping("/addNode")
    public String addNode(@RequestBody Node node) {
        // Compute a consistent hash for the node's name
        int hash = HashingFunction.hashNodeName(node.getName());

        // Prevent duplicate nodes with the same name (hash collision)
        if (nodeMap.containsKey(hash)) {
            return "Node with name already exists (hash collision): " + hash;
        }

        // Add the new node to the node map (hash → port)
        nodeMap.put(hash, node.getPort());
        saveNodeMapToDisk(); // Optional: store map on disk for persistence

        // ------------------------------
        // Handle local file registration
        // ------------------------------

        // Check if the node has any local file names listed
        if (node.getLocalFileNames() != null) {
            for (String filename : node.getLocalFileNames()) {

                // 1. Register the file as owned by this node
                fileToNodeMap.put(filename, hash);

                // 2. Add the file to the localFiles map under this node’s hash
                localFiles.computeIfAbsent(hash, k -> new ArrayList<>()).add(filename);

                // 3. Determine the replica node using consistent hashing
                int fileHash = HashingFunction.hashNodeName(filename);
                Integer replicaHash = nodeMap.floorKey(fileHash);
                if (replicaHash == null) replicaHash = nodeMap.lastKey(); // Wrap around

                // 4. If the replica is not the same as the owner, add to replicas
                if (!replicaHash.equals(hash)) {
                    replicas.computeIfAbsent(replicaHash, k -> new ArrayList<>()).add(filename);
                }

                // Optional: log for debugging
                System.out.println("Registered file: " + filename +
                        " → Owner hash: " + hash +
                        ", Replica hash: " + replicaHash);
            }
        }

        // Final response confirming the node was added
        return "Node added: " + node.getName() +
                " (hash: " + hash +
                ", Port: " + node.getPort() +
                ", Files: " + node.getLocalFileNames().size() + ")";
    }


    // Removes a node and its associated data
    @PostMapping("/removeNode")
    public String removeNode(@RequestBody Node node) {
        int hash = HashingFunction.hashNodeName(node.getName());

        if (!nodeMap.containsKey(hash)) {
            return "Node not found for removal: " + node.getName();
        }

        nodeMap.remove(hash);
        localFiles.remove(hash);
        replicas.remove(hash);
        fileToNodeMap.values().removeIf(value -> value == hash);
        saveNodeMapToDisk();

        return "Node removed: " + node.getName();
    }

    // Registers a file to a node and sets a replica based on the file hash
    @PostMapping("/registerFile")
    public String registerFile(@RequestParam String filename, @RequestParam String nodeName) {
        int nodeHash = HashingFunction.hashNodeName(nodeName);

        if (!nodeMap.containsKey(nodeHash)) {
            return "Node not registered: " + nodeName;
        }

        fileToNodeMap.put(filename, nodeHash);
        localFiles.computeIfAbsent(nodeHash, k -> new ArrayList<>()).add(filename);

        int fileHash = HashingFunction.hashNodeName(filename);
        Integer replicaNode = nodeMap.floorKey(fileHash);
        if (replicaNode == null) replicaNode = nodeMap.lastKey();

        if (!replicaNode.equals(nodeHash)) {
            replicas.computeIfAbsent(replicaNode, k -> new ArrayList<>()).add(filename);
        }

        return "File '" + filename + "' registered to node '" + nodeName + "' (hash: " + nodeHash + "), replica at node hash: " + replicaNode;
    }

    // Returns the port of the node that owns the given file
    @GetMapping("/getFileLocation")
    public String getFileLocation(@RequestParam String filename) {
        Integer nodeHash = fileToNodeMap.get(filename);
        if (nodeHash == null)
            return "File '" + filename + "' not registered.";

        int port = nodeMap.get(nodeHash);


        return "File location for '" + filename + "' → Node hash: " + nodeHash + " → Port: " + port;
    }

    // Uses hash fallback to find the best-fit node for the file (based on consistent hashing)
    @GetMapping("/getFileLocationHashed")
    public String getFileLocationHashed(@RequestParam String filename) {
        int fileHash = HashingFunction.hashNodeName(filename);
        Integer nodeHash = nodeMap.floorKey(fileHash);
        if (nodeHash == null) nodeHash = nodeMap.lastKey();

        int port = nodeMap.get(nodeHash);
        return "File hash = " + fileHash + ", routed to node hash: " + nodeHash + " → Port: " + port;
    }

    // Returns the full node map (hash → port)
    @GetMapping("/getAllNodes")
    public Map<Integer, Integer> getAllNodes() {
        return nodeMap;
    }

    // Returns local files owned by a specific node
    @GetMapping("/getLocalFiles")
    public List<String> getLocalFiles(@RequestParam String nodeName) {
        int hash = HashingFunction.hashNodeName(nodeName);
        return localFiles.getOrDefault(hash, Collections.emptyList());
    }

    // Returns replicated files for a specific node
    @GetMapping("/getReplicas")
    public List<String> getReplicas(@RequestParam String nodeName) {
        int hash = HashingFunction.hashNodeName(nodeName);
        return replicas.getOrDefault(hash, Collections.emptyList());
    }

    // Returns all nodes along with their local and replicated files
    @GetMapping("/getNodesWithFiles")
    public Map<String, Object> getNodesWithFiles() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : nodeMap.entrySet()) {
            Integer nodeHash = entry.getKey();
            int port = entry.getValue();

            Map<String, Object> nodeInfo = new LinkedHashMap<>();
            nodeInfo.put("port", port);
            nodeInfo.put("localFiles", localFiles.getOrDefault(nodeHash, Collections.emptyList()));
            nodeInfo.put("replicas", replicas.getOrDefault(nodeHash, Collections.emptyList()));

            result.put("NodeHash " + nodeHash, nodeInfo);
        }

        return result;
    }

    //Failure code
    // 1) DTO (data transfer object) for JSON response, this class can create an object that carries the data we want to send (port numbers)
    public static class NeighborResponse {
        private int previous, next;             //hold the port numbers
        public NeighborResponse() {}
        public NeighborResponse(int previous, int next) {
            this.previous = previous;
            this.next     = next;
        }
        public int getPrevious() { return previous; }
        public int getNext()     { return next;     }
        public void setPrevious(int p) { this.previous = p; }
        public void setNext(int n)     { this.next     = n; }
    }

    // 2) GET /neighbors?port={p} → returns new prev/next ports after p has failed
    // when a node fails, the node that detects this will call: GET http://<naming-server>/neighbors?port=p
    @GetMapping("/neighbors")
    public NeighborResponse getNeighbors(@RequestParam int port) {
        // find the hash key for the failed node
        int failedHash = nodeMap.entrySet().stream()
                //only keep the entry of the node with the matching portnumebr
                .filter(e -> e.getValue() == port)
                //get the key of this node
                .map(Map.Entry::getKey)
                //get the first and only hash wrapped in Optional<integer>
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown node port: " + port));

        // sort hashes to model the ring
        List<Integer> hashes = new ArrayList<>(nodeMap.keySet());
        Collections.sort(hashes);                       //we sort the list of keys in acsending way
        int idx       = hashes.indexOf(failedHash);     //locating the postion of the failed node
        int prevHash  = hashes.get((idx - 1 + hashes.size()) % hashes.size());  //computing the new predecessor and successor hashes
        int nextHash  = hashes.get((idx + 1)               % hashes.size());

        return new NeighborResponse(
                nodeMap.get(prevHash),      //we create the object Neighborresponse and pass the port numbers of the nieghbours, Spring Boot auto-serializes this into JSON like {"previous":3001,"next":3003}.
                nodeMap.get(nextHash)
        );
    }

    // 3) DELETE /nodes?port={p} → evict p from the ring map
    @DeleteMapping("/nodes")
    public void removeNode(@RequestParam int port) {
        nodeMap.values().removeIf(p -> p == port);
    }


//
//    // Placeholder
//    private void saveNodeMapToDisk() {
//        // TODO: implement if needed
//    }
}