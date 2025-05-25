package Namingserver.namingserver.controller;


import Namingserver.namingserver.controller.communication.ServerUnicastSender;
import NodePackage.Node;
import Functions.HashingFunction;
import NodePackage.NodeApp;
import NodePackage.NodeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import NodePackage.NodeRequest;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
public class ServerController {

    // Max and min values used for boundary checks (currently unused)
    private static final int MAX = Integer.MAX_VALUE;
    private static final int MIN = -Integer.MAX_VALUE;

    // Maps hashed node IDs to IP addresses
    private static TreeMap<Integer, Integer> nodeMap = new TreeMap<>();



    // Maps IP addresses to node names (to reconstruct full Node objects)
    private Map<String, String> ipToName = new HashMap<>();

    private Map<Integer, String> hashToName = new HashMap<>();




    // Maps file names to node hashes (ownership)
    private Map<String, Integer> fileToNodeMap = new HashMap<>();

    // Stores the actual file names per node
//    private Map<Integer, List<String>> localFiles = new HashMap<>();

    // Stores replicated files per node
//    private Map<Integer, List<String>> replicas = new HashMap<>();
    private static final Map<Integer, List<String>> localFiles = new HashMap<>();
    private static final Map<Integer, List<String>> replicas = new HashMap<>();
//

    // Writes the current state of the node map to a file on disk
    private void saveNodeMapToDisk() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("nodeMap.json"), nodeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


     // REPLICATION FIX
    public String addNodeFromMulticast(Node node, List<String> localFileNames) {
        int hash = HashingFunction.hashNodeName(node.getName());

        // Prevent duplicate nodes with the same name (hash collision)
        if (nodeMap.containsKey(hash)) {
            return "Node with name already exists (hash collision): " + hash;
        }
        nodeMap.put(hash, node.getPort());
        saveNodeMapToDisk();
        saveNodeMapToDisk(); // Optional: store map on disk for persistence

        // ------------------------------
        // Handle local file registration
        // ------------------------------

        // Check if the node has any local file names listed

        if (localFileNames != null) {

            for (String filename : localFileNames) {

                // 1. Register the file as owned by this node
                fileToNodeMap.put(filename, hash);

                // 2. Add the file to the localFiles map under this node’s hash
                localFiles.computeIfAbsent(hash, k -> new ArrayList<>()).add(filename);

                // 3. Determine the replica node using consistent hashing
                int fileHash = HashingFunction.hashNodeName(filename);
                Integer replicaHash = nodeMap.floorKey(fileHash);
                if (replicaHash == null) replicaHash = nodeMap.lastKey(); // Wrap around
                System.out.println("Filehash:" + fileHash);
                if (replicaHash.equals(hash)) {
                    replicaHash = nodeMap.floorKey(replicaHash - 1);
                    if (replicaHash == null) replicaHash = nodeMap.lastKey(); // Wrap around
                }




                // 4. If the replica is not the same as the owner, add to replicas
                System.out.println(">> Checking replicaHash vs hash: " + replicaHash + " vs " + hash);
                replicas.computeIfAbsent(replicaHash, k -> new ArrayList<>()).add(filename);//Voegt de bestandsnaam toe aan de replicas-mapping van die replica-node.

                //  Stuur replica instructie via UDP
                int originalNodePort = node.getPort();
                int replicaNodePort = nodeMap.get(replicaHash);

                System.out.println(">> sendReplicaInstruction() called!");

                ServerUnicastSender.sendReplicaInstruction(String.valueOf(originalNodePort), filename, String.valueOf(replicaNodePort));




                // Optional: log for debugging
                System.out.println("Registered file: " + filename +
                        " → Owner hash: " + hash +
                        ", Replica hash: " + replicaHash);
            }
        }

        // Final response confirming the node was added
        return "Node added: " + node.getName() +
                " (hash: " + hash +
                ", Port: " + node.getPort() + ")";

    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    @PostMapping("/lock")
    public String lockFile(@RequestParam String filename, @RequestParam String requesterName) {
        Integer ownerHash = fileToNodeMap.get(filename);
        if (ownerHash == null) return "❌ File not found.";

        int requesterHash = HashingFunction.hashNodeName(requesterName);
        if (requesterHash != ownerHash) return "🚫 Only the owner can lock the file.";

        // Lock instructie sturen naar eigenaar (requester)
        int ownerPort = nodeMap.get(ownerHash);
        ServerUnicastSender.sendLockInstruction(String.valueOf(ownerPort), filename, true);  // true = lock
        System.out.println("🔐 Lock instructie gestuurd naar eigenaar op port " + ownerPort);

        // Lock instructie sturen naar alle replica-nodes
        List<Integer> replicaHashes = replicas.entrySet().stream()
                .filter(entry -> entry.getValue().contains(filename))
                .map(Map.Entry::getKey)
                .toList();

        for (Integer replicaHash : replicaHashes) {
            if (replicaHash == ownerHash) continue; // skip als replica toevallig owner is
            int replicaPort = nodeMap.get(replicaHash);
            ServerUnicastSender.sendLockInstruction(String.valueOf(replicaPort), filename, true);
            System.out.println("🔐 Lock instructie gestuurd naar replica op port " + replicaPort);
        }

        return "✅ Lock instructies verzonden voor bestand: " + filename;
    }

    @PostMapping("/unlock")
    public String unlockFile(@RequestParam String filename, @RequestParam String requesterName) {
        Integer ownerHash = fileToNodeMap.get(filename);
        if (ownerHash == null) return "❌ File not found.";

        int requesterHash = HashingFunction.hashNodeName(requesterName);
        if (requesterHash != ownerHash) return "🚫 Only the owner can unlock the file.";

        // Unlock instructie naar owner
        int ownerPort = nodeMap.get(ownerHash);
        ServerUnicastSender.sendLockInstruction(String.valueOf(ownerPort), filename, false); // false = unlock
        System.out.println("🔓 Unlock instructie gestuurd naar eigenaar op port " + ownerPort);

        // Unlock instructie naar replicas
        List<Integer> replicaHashes = replicas.entrySet().stream()
                .filter(entry -> entry.getValue().contains(filename))
                .map(Map.Entry::getKey)
                .toList();

        for (Integer replicaHash : replicaHashes) {
            if (replicaHash == ownerHash) continue;
            int replicaPort = nodeMap.get(replicaHash);
            ServerUnicastSender.sendLockInstruction(String.valueOf(replicaPort), filename, false);
            System.out.println("🔓 Unlock instructie gestuurd naar replica op port " + replicaPort);
        }

        return "✅ Unlock instructies verzonden voor bestand: " + filename;
    }

    @PostMapping("/addNode")
    public ResponseEntity<String> addNode(@RequestBody NodeRequest request) {
        try {
            int port = request.getPort();
            String name = request.getName();
            String localPath = request.getLocalPath();
            String replicaPath = request.getReplicaPath();


            int hash = HashingFunction.hashNodeName(name);

            // Store hash to name mapping
            hashToName.put(hash, name);

            System.out.println("Received REST node creation request: " + name);

            NodeApp app = new NodeApp();
            Node node = app.createAndAnnounceNewNode(name, port, localPath, replicaPath);

            return ResponseEntity.ok("Node created: " + node.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating node: " + e.getMessage());
        }
    }




    @PostMapping("/removeNode")
    public ResponseEntity<String> removeNode(@RequestBody Node node) {
        new Thread(() -> {
            try {
                Thread.sleep(500); // small delay to allow GUI to receive response
                NodeApp.shutdownGracefully(node); // shuts down the node
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok("Node shutdown initiated.");
    }


    // Registers a file to a node and sets a replica based on the file hash
    @PostMapping("/registerFile")
    public String registerFile(@RequestParam String filename, @RequestParam String nodeName) {
        int nodeHash = HashingFunction.hashNodeName(nodeName);

        if (!nodeMap.containsKey(nodeHash)) {
            return "Node not registered: " + nodeName;
        }

        // Register ownership
        fileToNodeMap.put(filename, nodeHash);
        localFiles.computeIfAbsent(nodeHash, k -> new ArrayList<>()).add(filename);

        // 3. Determine the replica node using consistent hashing
        int fileHash = HashingFunction.hashNodeName(filename);
        Integer replicaNode = nodeMap.floorKey(fileHash);
        if (replicaNode == null) replicaNode = nodeMap.lastKey(); // Wrap around
        System.out.println("Filehash:" + fileHash);
        if (replicaNode.equals(nodeHash)) {
            replicaNode = nodeMap.floorKey(replicaNode - 1);
            if (replicaNode == null) replicaNode = nodeMap.lastKey(); // Wrap around
        }




        // 4. If the replica is not the same as the owner, add to replicas
        System.out.println(">> Checking replicaHash vs hash: " + replicaNode + " vs " + nodeHash);
        replicas.computeIfAbsent(replicaNode, k -> new ArrayList<>()).add(filename);//Voegt de bestandsnaam toe aan de replicas-mapping van die replica-node.

        //  Stuur replica instructie via UDP
        int originalNodePort = nodeMap.get(nodeHash);
        int replicaNodePort = nodeMap.get(replicaNode);

        System.out.println(">> sendReplicaInstruction() called!");

        ServerUnicastSender.sendReplicaInstruction(String.valueOf(originalNodePort), filename, String.valueOf(replicaNodePort));

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
//    @GetMapping("/getAllNodes")
    @GetMapping("/getAllNodes")
    public Map<Integer, Integer> getAllNodes() {
        return nodeMap;
    }



    // Returns local files owned by a specific node
    @GetMapping("/getLocalFiles")
    public List<String> getLocalFiles(@RequestParam int nodePort) {
        Optional<Map.Entry<Integer, Integer>> entry = nodeMap.entrySet().stream()
                .filter(e -> e.getValue() == nodePort)
                .findFirst();

        if (entry.isPresent()) {
            int hash = entry.get().getKey();
            return localFiles.getOrDefault(hash, Collections.emptyList());
        }

        return Collections.emptyList();
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



    // SHUTDOWN

    // Graceful shutdown: node calls this before terminating
    @GetMapping("/shutdown")
    public Map<String, Integer> shutdown(@RequestParam int port) {
        // 1. Find node's hash
        Integer shuttingDownHash = nodeMap.entrySet().stream()
                .filter(e -> e.getValue().equals(port))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (shuttingDownHash == null) {
            return Map.of("error", -1);  // not found
        }

        // 2. Get neighbor hashes
        List<Integer> hashes = new ArrayList<>(nodeMap.keySet());
        Collections.sort(hashes);
        int idx = hashes.indexOf(shuttingDownHash);
        int prevHash = hashes.get((idx - 1 + hashes.size()) % hashes.size());
        int nextHash = hashes.get((idx + 1) % hashes.size());

        int prevPort = nodeMap.get(prevHash);
        int nextPort = nodeMap.get(nextHash);

        // 3. Remove node
        nodeMap.remove(shuttingDownHash);
        localFiles.remove(shuttingDownHash);
        replicas.remove(shuttingDownHash);
        fileToNodeMap.values().removeIf(v -> v == shuttingDownHash);
        saveNodeMapToDisk();

        System.out.printf("Node on port %d gracefully shut down. Neighbors: prev=%d, next=%d%n",
                port, prevPort, nextPort);

        return Map.of(
                "prevPort", prevPort,
                "nextPort", nextPort
        );
    }

    @PostMapping("/updateReplicaAfterShutdown")
    public ResponseEntity<String> updateReplicaAfterShutdown(
            @RequestParam String file,
            @RequestParam int oldPort,
            @RequestParam int newPort) {

        // Step 1: Get the old node hash
        Optional<Map.Entry<Integer, Integer>> oldEntry = nodeMap.entrySet().stream()
               // .filter(e -> e.getValue() == oldPort)
                .filter(e -> e.getValue().equals(oldPort))
                .findFirst();

        if (oldEntry.isEmpty()) {
            return ResponseEntity.status(404).body("Old node not found in nodeMap");
        }

        int oldHash = oldEntry.get().getKey();

        // Step 2: Remove the file from old node's replica list
        replicas.getOrDefault(oldHash, new ArrayList<>()).remove(file);

        // Step 3: Get the new node hash
        Optional<Map.Entry<Integer, Integer>> newEntry = nodeMap.entrySet().stream()
                .filter(e -> e.getValue() == newPort)
                .findFirst();

        if (newEntry.isEmpty()) {
            return ResponseEntity.status(404).body("New node not found in nodeMap");
        }

        int newHash = newEntry.get().getKey();

        // Step 4: Add the file to new node's replica list
        replicas.computeIfAbsent(newHash, k -> new ArrayList<>());

        if (!replicas.get(newHash).contains(file)) {
            replicas.get(newHash).add(file);
        }

        // ✅ Confirm in logs
        System.out.println("📝 Replica reassignment completed for file '" + file + "'");
        System.out.println("   Removed from node hash: " + oldHash + ", port: " + oldPort);
        System.out.println("   Added to node hash: " + newHash + ", port: " + newPort);

        return ResponseEntity.ok("Replica log updated");
    }


    // THIS IS CODE FOR SHUTDOWN PHASE 3
//    @GetMapping("/getReplicaForFile")
//    public ResponseEntity<Integer> getReplicaForFile(
//            @RequestParam String filename,
//            @RequestParam int shuttingDownPort) {
//
//        int shuttingDownHash = nodeMap.entrySet().stream()
//                .filter(e -> e.getValue().equals(shuttingDownPort))
//                .map(Map.Entry::getKey)
//                .findFirst()
//                .orElse(-1);
//
//        for (Map.Entry<Integer, List<String>> entry : replicas.entrySet()) {
//            int nodeHash = entry.getKey();
//            if (nodeHash == shuttingDownHash) continue; // skip shutting down node
//            if (entry.getValue().contains(filename)) {
//                Integer port = nodeMap.get(nodeHash);
//                if (port != null) {
//                    return ResponseEntity.ok(port);
//                }
//            }
//        }
//        return ResponseEntity.ok(null);
//    }



    @GetMapping("/nodeCount")
    public ResponseEntity<Integer> getNodeCountFromServer() {
        return ResponseEntity.ok(getNodeMap().size());
    }

    @GetMapping("/getFilesForNode")
    public ResponseEntity<Map<String, List<String>>> getFilesForNode(@RequestParam String nodeName) {
        int hash = HashingFunction.hashNodeName(nodeName);

        List<String> localList = localFiles.getOrDefault(hash, new ArrayList<>());
        List<String> replicaList = replicas.getOrDefault(hash, new ArrayList<>());

        Map<String, List<String>> result = new HashMap<>();
        result.put("local", localList);
        result.put("replica", replicaList);

        return ResponseEntity.ok(result);
    }


//
//    // Placeholder
//    private void saveNodeMapToDisk() {
//        // TODO: implement if needed
//    }






    public NavigableMap<Integer, Integer> getNodeMap() {
        return nodeMap;
    }

    public Map<Integer, List<String>> getLocalFiles() {
        return localFiles;
    }

    public Map<Integer, List<String>> getReplicas() {

        return replicas;
    }

    public Map<String, Integer> getFileToNodeMap() {
        return fileToNodeMap;
    }

    public String getNodeNameByHash(int hash) {
        return hashToName.getOrDefault(hash, "Unknown");
    }

    @GetMapping("/getNodeName")
    public String getNodeName(@RequestParam int hash) {
        return getNodeNameByHash(hash);
    }




}