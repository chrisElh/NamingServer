package gui;

import NodePackage.Node;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;




import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import NodePackage.NodeApp;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
                for (String entry : entries) {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        int hash = Integer.parseInt(parts[0].trim());
                        int port = Integer.parseInt(parts[1].trim());
                        String name = "Node@" + port;
                        nodeList.add(new NodeDisplay(name, port, hash));
                    }
                }

                nodeTable.setItems(nodeList);

            } else {
                showAlert("Server returned HTTP " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to fetch nodes: " + e.getMessage());
        }
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