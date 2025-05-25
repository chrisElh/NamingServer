package gui;

import NodePackage.Node;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;




import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import NodePackage.NodeApp;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;




public class MainController {


    @FXML
    private TextField nodeNameField;
    @FXML
    private ChoiceBox<Integer> fileCountChoiceBox;

    private final NodeApp app = new NodeApp();
    private static int nodeCreationIndex = 1;


    @FXML
    private void initialize() {
        fileCountChoiceBox.getItems().addAll(0, 1, 2, 3, 4, 5);
        fileCountChoiceBox.setValue(0); // default selection



        //
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        hashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));


        nodeTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                handleTableClick();  // triggers file fetch
            }
        });



//        nodeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
//            if (newSelection != null) {
//                String selectedNodeName = newSelection.getName(); // or getNodeName() if it's called that
//                fetchFilesForNode(selectedNodeName);
//            }
//        });
//        nodeTable2.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
//            if (newSelection != null) {
//                String selectedNodeName = newSelection.getName();
//                fetchFilesForNode(selectedNodeName);
//            }
//        });




    }

    @FXML
    private void handleAddNode() {
        String name = nodeNameField.getText().trim();
        Integer fileCount = fileCountChoiceBox.getValue();

        if (name.isEmpty()) {
            showAlert("Please enter a node name.");
            return;
        }

        int port = generateRandomPort();
//        int index = nodeCreationIndex++; // Static counter

        int index = fetchNodeCountFromServer() + 1;
//
//        int index = Node.getTotalNodes();
        String basePath = "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Lab_7/GUI_Lab_7/namingserver/src/main/resources/";
        String localPath = basePath + "files" + index + "/local";
        String replicaPath = basePath + "files" + index + "/replica";

        // Ensure folders exist
        new File(localPath).mkdirs();
        new File(replicaPath).mkdirs();

        try {
            generateFiles(localPath, fileCount);  // Create .txt files in local folder

            // === SEND TO NAMING SERVER VIA REST ===
            String jsonBody = String.format("""
                    {
                      "name": "%s",
                      "port": %d,
                      "localPath": "%s",
                      "replicaPath": "%s"
                    }
                    """, name, port, localPath, replicaPath);

            String namingServerIP = "localhost";  // or use actual IP of G4c1.6dist
            URL url = new URL("http://" + namingServerIP + ":8080/addNode");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                showAlert("Node created: " + name + " (port: " + port + ")\nFiles: " + listLocalFiles(localPath));
            } else {
                showAlert("Server error (HTTP " + responseCode + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed: " + e.getMessage());
        }

        nodeNameField.clear();
        fileCountChoiceBox.setValue(0);
    }

//    private static int nodeCreationIndex = 1;

    private int generateRandomPort() {
        return (new Random().nextInt(900) + 100) * 10;
    }

    private void generateFiles(String folderPath, int count) throws IOException {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();

        for (int i = 0; i < count; i++) {
            String filename = random.ints(8, 0, chars.length())
                    .mapToObj(index -> String.valueOf(chars.charAt(index)))
                    .collect(Collectors.joining()) + ".txt";

            File file = new File(folderPath, filename);
            if (file.createNewFile()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("Auto-generated file: " + filename);
                }
            }
        }
    }

    private String listLocalFiles(String folderPath) {
        File folder = new File(folderPath);
        String[] files = folder.list((dir, name) -> name.endsWith(".txt"));
        return (files != null) ? Arrays.toString(files) : "[]";
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private int fetchNodeCountFromServer() {
        try {
            URL url = new URL("http://localhost:8080/nodeCount");  // Use IP if on remote G4c1
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line = reader.readLine();
                    return Integer.parseInt(line);
                }
            } else {
                showAlert("Failed to fetch node count. Defaulting to 0.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error contacting server. Assuming 0 nodes.");
        }

        return 0;
    }

// Table view  GUI

    @FXML private TableView<NodeDisplay> nodeTable;
    @FXML private TableView<NodeDisplay> nodeTable2;
    @FXML private TableColumn<NodeDisplay, String> nameColumn;
    @FXML private TableColumn<NodeDisplay, Integer> portColumn;
    @FXML private TableColumn<NodeDisplay, Integer> hashColumn;


    public static class NodeDisplay {
        private final String name;
        private final int port;
        private final int hash;

        public NodeDisplay(String name, int port, int hash) {
            this.name = name;
            this.port = port;
            this.hash = hash;
        }

        public String getName() { return name; }
        public int getPort() { return port; }
        public int getHash() { return hash; }
    }



    @FXML
    private void handleFetchNodes() {
        try {
            URL url = new URL("http://localhost:8080/getAllNodes");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }
                }

                // Parse JSON manually (e.g., {"12345":8080, "45678":9060})
                String cleaned = json.toString().replaceAll("[{}\"]", "");
                String[] entries = cleaned.split(",");

                ObservableList<NodeDisplay> nodeList = FXCollections.observableArrayList();
                ObservableList<NodeDisplay> nodeList2 = FXCollections.observableArrayList();

                for (String entry : entries) {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        int hash = Integer.parseInt(parts[0].trim());
                        int port = Integer.parseInt(parts[1].trim());
                        String name = "Node@" + port;
                        String RealName = fetchNodeNameByHash(hash); // <-- Get the real name!

                        nodeList.add(new NodeDisplay(RealName, port, hash));
                        nodeList2.add(new NodeDisplay(name, port, hash));
                    }
                }

                nodeTable.setItems(nodeList);
//                nodeTable2.setItems(nodeList2);

            } else {
                showAlert("Server returned HTTP " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to fetch nodes: " + e.getMessage());
        }
    }



    // FIle viewr

    @FXML
    private ListView<String> localFileListView;
    @FXML
    private ListView<String> replicaFileListView;



    @FXML
    private void handleTableClick() {
        NodeDisplay selected = nodeTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String nodeName = selected.getName();

        fetchConfigForNode(selected.getName(), selected.getPort());

        try {
            URL url = new URL("http://localhost:8080/getFilesForNode?nodeName=" + nodeName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int code = con.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String json = reader.lines().collect(Collectors.joining());

                Map<String, List<String>> fileMap = parseFileJson(json);

                localFileListView.setItems(FXCollections.observableArrayList(fileMap.get("local")));
                replicaFileListView.setItems(FXCollections.observableArrayList(fileMap.get("replica")));
            } else {
                showAlert("Failed to fetch file list (HTTP " + code + ")");
            }




        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error: " + e.getMessage());
        }
    }


    private Map<String, List<String>> parseFileJson(String json) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("local", new ArrayList<>());
        map.put("replica", new ArrayList<>());

        try {
            // Clean and standardize JSON string
            json = json.replaceAll("[\\{\\}\"]", "");  // Remove curly braces and quotes
            String[] sections = json.split("(?=local:|replica:)");  // split by key names

            for (String section : sections) {
                if (section.startsWith("local:")) {
                    String files = section.substring("local:".length()).trim();
                    if (!files.isEmpty()) {
                        List<String> localList = Arrays.stream(files.split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList();
                        map.put("local", localList);
                    }
                } else if (section.startsWith("replica:")) {
                    String files = section.substring("replica:".length()).trim();
                    if (!files.isEmpty()) {
                        List<String> replicaList = Arrays.stream(files.split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList();
                        map.put("replica", replicaList);
                    }
                }
            }

            // Debugging
//            System.out.println("Local files: " + map.get("local"));
//            System.out.println("Replica files: " + map.get("replica"));

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error parsing file JSON: " + e.getMessage());
        }

        return map;
    }


    private Map<String, List<String>> parseFileLists(String json) {
        Map<String, List<String>> map = new HashMap<>();

        try {
            json = json.replace("{", "").replace("}", "").replace("\"", "");
            String[] parts = json.split(",");

            List<String> local = new ArrayList<>();
            List<String> replica = new ArrayList<>();

            for (String part : parts) {
                if (part.contains("local")) {
                    local.add(part.substring(part.indexOf(":") + 1).trim());
                } else if (part.contains("replica")) {
                    replica.add(part.substring(part.indexOf(":") + 1).trim());
                } else {
                    // If multiple files, handle them here (improvement recommended: use a real JSON parser like Gson)
                    if (!part.trim().isEmpty()) {
                        if (map.containsKey("local")) {
                            local.add(part.trim());
                        } else {
                            replica.add(part.trim());
                        }
                    }
                }
            }

            map.put("local", local);
            map.put("replica", replica);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }


//    @FXML
//    private TextArea localFilesArea;
//
//    @FXML
//    private TextArea replicaFilesArea;


//    private void fetchFilesForNode(String nodeName) {
//        try {
//            String namingServerIP = "localhost";  // or your remote host
//            URL url = new URL("http://" + namingServerIP + ":8080/getFilesForNode?nodeName=" + nodeName);
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setRequestMethod("GET");
//
//            int responseCode = con.getResponseCode();
//            if (responseCode == 200) {
//                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                String json = in.lines().collect(Collectors.joining());
//                in.close();
//
//                // Parse JSON
//                Map<String, List<String>> result = parseFileLists(json);
//
//                localFilesArea.setText(String.join("\n", result.getOrDefault("local", List.of())));
//                replicaFilesArea.setText(String.join("\n", result.getOrDefault("replica", List.of())));
//
//            } else {
//                showAlert("Server error (HTTP " + responseCode + ")");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            showAlert("Error fetching files: " + e.getMessage());
//        }
//    }







    //fetching the node names by hash
    private String fetchNodeNameByHash(int hash) {
        try {
            URL url = new URL("http://localhost:8080/getNodeName?hash=" + hash);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    return reader.readLine();
                }
            } else {
                showAlert("Failed to fetch node name (HTTP " + responseCode + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error contacting server for node name.");
        }
        return "Unknown";
    }



    // View Next and previous nodes

    @FXML private Label prevIdLabel;
    @FXML private Label nextIdLabel;



    private void fetchConfigForNode(String nodeName, int nodePort) {
        try {
            URL url = new URL("http://localhost:8080/neighbors?port=" + nodePort);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int code = con.getResponseCode();
            if (code == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String json = in.lines().collect(Collectors.joining());
                in.close();

                Map<String, Integer> config = parseNeighborJson(json);
                prevIdLabel.setText(String.valueOf(config.getOrDefault("previous", -1)));
                nextIdLabel.setText(String.valueOf(config.getOrDefault("next", -1)));
            } else {
                showAlert("Server returned: " + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error fetching node neighbors: " + e.getMessage());
        }
    }


    private Map<String, Integer> parseNeighborJson(String json) {
        Map<String, Integer> map = new HashMap<>();
        json = json.replaceAll("[{}\\s\"]", "");  // remove { } " and spaces
        String[] parts = json.split(",");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                map.put(kv[0], Integer.parseInt(kv[1]));
            }
        }
        return map;
    }

// Removing Node
@FXML
private void handleShutdownNode() {
    NodeDisplay selected = nodeTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
        showAlert("Please select a node to shut down.");
        return;
    }

    // Run shutdown logic in background thread
    new Thread(() -> {
        try {
            URL url = new URL("http://localhost:8080/removeNode");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            // Optional: Timeout settings to avoid hanging
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            String jsonBody = String.format("""
                {
                    "name": "%s",
                    "port": %d
                }
                """, selected.getName(), selected.getPort());

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }

            int code = con.getResponseCode();
            if (code == 200) {
                Platform.runLater(() -> {
                    showAlert("Node shut down: " + selected.getName());
                    handleFetchNodes();  // Refresh node list on GUI
                });
            } else {
                Platform.runLater(() ->
                        showAlert("Failed to shut down node (HTTP " + code + ")")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    showAlert("Error during shutdown: " + e.getMessage())
            );
        }
    }).start();
}


}













//    @FXML private TextField nodeNameField;
//    @FXML private TextField nodePortField;
//
//    private final NodeApp app = new NodeApp();
//
//    @FXML
//    private void handleAddNode() {
//        String name = nodeNameField.getText().trim();
//        String portStr = nodePortField.getText().trim();
//
//        if (name.isEmpty() || portStr.isEmpty()) {
//            showAlert("Please fill in both fields.");
//            return;
//        }
//
//        int port;
//        try {
//            port = Integer.parseInt(portStr);
//        } catch (NumberFormatException e) {
//            showAlert("Port must be a number.");
//            return;
//        }
//
//        // Compute paths dynamically from known structure
//        String basePath = "/home/tadiwos/6th_Semester/Distributed_Systems/Labs/Testing/namingserver/src/main/resources/";
//        String localPath = basePath + "files" + getFileIndex(port) + "/local";
//        String replicaPath = basePath + "files" + getFileIndex(port) + "/replica";
//
//        try {
//            Node node = app.createAndAnnounceNewNode(name, port, localPath, replicaPath);
//            showAlert("Node created: " + node.getName() + " with files:\n" + node.getLocalFileNames());
//        } catch (Exception e) {
//            e.printStackTrace();
//            showAlert("Failed to add node:\n" + e.getMessage());
//        }
//
//        nodeNameField.clear();
//        nodePortField.clear();
//    }
//
//    private int getFileIndex(int port) {
//        // You can make this smarter or map it in a different way
//        return switch (port) {
//            case 3030 -> 1;
//            case 9060 -> 2;
//            case 4060 -> 3;
//            case 3700 -> 4;
//            default -> 1; // fallback
//        };
//    }
//
//    private void showAlert(String msg) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Info");
//        alert.setHeaderText(null);
//        alert.setContentText(msg);
//        alert.showAndWait();
//    }

//}