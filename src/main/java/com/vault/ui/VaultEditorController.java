package com.vault.ui;

import com.vault.crypto.CryptoEngine;
import com.vault.db.DatabaseManager;
import com.vault.db.VaultEntryRepository;
import com.vault.db.GitRepositoryRepository;
import com.vault.model.UnlockedVault;
import com.vault.model.VaultEntry;
import com.vault.model.GitRepository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * VaultEditorController — Manages environment variable keys for a repository.
 * Core CRUD operations: Create, Read, Update, Delete secrets.
 */
public class VaultEditorController {

    @FXML
    private VBox editorContainer;
    @FXML
    private Label projectTitleLabel;

    @FXML
    private TableView<VaultEntry> secretsTable;
    @FXML
    private TableColumn<VaultEntry, String> keyColumn;
    @FXML
    private TableColumn<VaultEntry, String> valueColumn;
    @FXML
    private TableColumn<VaultEntry, Void> actionsColumn;

    private GitRepository currentRepo;
    private UnlockedVault session;
    private VaultEntryRepository vaultRepo;
    private ObservableList<VaultEntry> entriesModel;
    private File currentEnvFile;  // Track the current .env file being edited

    @FXML
    public void initialize() {
        vaultRepo = new VaultEntryRepository(DatabaseManager.getInstance().getConnection());
        entriesModel = FXCollections.observableArrayList();

        // Tell the table how to extract data from the VaultEntry object
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("keyName"));

        // Bind valueColumn to decryptedValue property
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("decryptedValue"));
        
        // Simple cell factory for the value column - just display the value as-is
        valueColumn.setCellFactory(tc -> new TableCell<VaultEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setStyle("-fx-text-fill: #f1fa8c;");
            }
        });

        // Actions column with Update and Delete buttons
        actionsColumn.setCellFactory(tc -> new TableCell<VaultEntry, Void>() {
            private final Button updateBtn = new Button("✏ Update");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox container = new HBox(5, updateBtn, deleteBtn);
            
            {
                updateBtn.setStyle("-fx-text-fill: #f1fa8c; -fx-font-size: 10;");
                deleteBtn.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 10;");
                updateBtn.setOnAction(e -> handleUpdateSecret(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteFromTable(getTableRow().getItem()));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });

        secretsTable.setItems(entriesModel);
    }

    /**
     * Called by DashboardController immediately after loading this view.
     * Works with repository instead of project.
     */
    public void initDataForRepo(GitRepository repo, UnlockedVault session) {
        this.currentRepo = repo;
        this.session = session;
        projectTitleLabel.setText("Keys for: " + repo.getName());
        loadSecrets();
    }

    private void loadSecrets() {
        entriesModel.clear();
        if (currentRepo == null) {
            System.out.println("[VaultEditor] No repository selected");
            return;
        }

        try {
            // Read the .env file directly from the repository
            currentEnvFile = findEnvFile(new File(currentRepo.getLocalPath()));
            if (currentEnvFile == null) {
                System.out.println("[VaultEditor] .env file not found in repository: " + currentRepo.getLocalPath());
                new Alert(Alert.AlertType.WARNING, ".env file not found in:\n" + currentRepo.getLocalPath()).showAndWait();
                return;
            }

            System.out.println("[VaultEditor] Reading .env from: " + currentEnvFile.getAbsolutePath());
            String envContent = new String(Files.readAllBytes(currentEnvFile.toPath()));
            String[] lines = envContent.split("\n");
            System.out.println("[VaultEditor] .env file contains " + lines.length + " lines");

            int loadedCount = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String keyName = parts[0].trim();
                    String value = parts[1].trim();

                    // Create a VaultEntry with the actual value
                    VaultEntry entry = new VaultEntry(currentRepo.getId(), keyName, "", "");
                    entry.setDecryptedValue(value);
                    entriesModel.add(entry);
                    loadedCount++;
                }
            }

            System.out.println("[VaultEditor] Loaded " + loadedCount + " environment variables from .env file");
            if (loadedCount == 0) {
                new Alert(Alert.AlertType.INFORMATION, ".env file found but contains no valid KEY=VALUE pairs").showAndWait();
            }
        } catch (IOException e) {
            System.err.println("Failed to read .env file: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Failed to read .env file: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddSecret(ActionEvent event) {
        if (currentEnvFile == null) {
            new Alert(Alert.AlertType.WARNING, "No .env file loaded. Please select a repository first.").showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("API_KEY=my_secret_token");
        dialog.setTitle("Add Secret");
        dialog.setHeaderText("Add a new environment variable");
        dialog.setContentText("Format: KEY=VALUE");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            String[] parts = input.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                try {
                    // Read current .env content
                    String envContent = new String(Files.readAllBytes(currentEnvFile.toPath()));
                    
                    // Append new secret to .env file
                    String newLine = key + "=" + value + "\n";
                    Files.write(currentEnvFile.toPath(), newLine.getBytes(), StandardOpenOption.APPEND);
                    
                    System.out.println("[VaultEditor] Added secret: " + key);
                    new Alert(Alert.AlertType.INFORMATION, "Secret added: " + key).showAndWait();
                    loadSecrets();
                } catch (IOException e) {
                    new Alert(Alert.AlertType.ERROR, "Failed to add secret: " + e.getMessage()).showAndWait();
                    e.printStackTrace();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid format. Please use KEY=VALUE").showAndWait();
            }
        });
    }

    @FXML
    public void handleDeleteSecret(ActionEvent event) {
        VaultEntry selected = secretsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            vaultRepo.deleteEntry(selected.getId());
            loadSecrets();
        }
    }

    /**
     * Updates an existing secret (edit value for existing key)
     */
    private void handleUpdateSecret(VaultEntry entry) {
        TextInputDialog dialog = new TextInputDialog(entry.getDecryptedValue());
        dialog.setTitle("Update Secret");
        dialog.setHeaderText("Update the value for: " + entry.getKeyName());
        dialog.setContentText("New value:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newValue -> {
            try {
                // Read current .env content
                String envContent = new String(Files.readAllBytes(currentEnvFile.toPath()));
                String[] lines = envContent.split("\n", -1);  // -1 to preserve trailing empty lines
                
                // Find and replace the line with matching key
                StringBuilder updatedContent = new StringBuilder();
                boolean found = false;
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                        String[] parts = trimmedLine.split("=", 2);
                        if (parts.length == 2 && parts[0].trim().equals(entry.getKeyName())) {
                            updatedContent.append(entry.getKeyName()).append("=").append(newValue).append("\n");
                            found = true;
                            continue;
                        }
                    }
                    updatedContent.append(line).append("\n");
                }
                
                if (found) {
                    // Remove last newline that was added
                    String content = updatedContent.toString();
                    if (content.endsWith("\n")) {
                        content = content.substring(0, content.length() - 1);
                    }
                    Files.write(currentEnvFile.toPath(), content.getBytes());
                    System.out.println("[VaultEditor] Updated secret: " + entry.getKeyName());
                    new Alert(Alert.AlertType.INFORMATION, "Secret updated: " + entry.getKeyName()).showAndWait();
                    loadSecrets();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Secret not found in .env file").showAndWait();
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to update secret: " + e.getMessage()).showAndWait();
                e.printStackTrace();
            }
        });
    }

    /**
     * Deletes secret from table row action
     */
    private void handleDeleteFromTable(VaultEntry entry) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, 
                "Delete " + entry.getKeyName() + "?", 
                ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Confirm Delete");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                // Read current .env content
                String envContent = new String(Files.readAllBytes(currentEnvFile.toPath()));
                String[] lines = envContent.split("\n", -1);  // -1 to preserve trailing empty lines
                
                // Filter out the line with matching key
                StringBuilder updatedContent = new StringBuilder();
                boolean found = false;
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                        String[] parts = trimmedLine.split("=", 2);
                        if (parts.length == 2 && parts[0].trim().equals(entry.getKeyName())) {
                            found = true;
                            continue;  // Skip this line (delete it)
                        }
                    }
                    updatedContent.append(line).append("\n");
                }
                
                if (found) {
                    // Remove last newline that was added
                    String content = updatedContent.toString();
                    if (content.endsWith("\n")) {
                        content = content.substring(0, content.length() - 1);
                    }
                    Files.write(currentEnvFile.toPath(), content.getBytes());
                    System.out.println("[VaultEditor] Deleted secret: " + entry.getKeyName());
                    loadSecrets();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Secret not found in .env file").showAndWait();
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to delete secret: " + e.getMessage()).showAndWait();
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an encrypted backup of the .env file at a predefined safe location
     * Searches for .env file in the repository root and subdirectories
     */
    @FXML
    public void handleBackupEnv(ActionEvent event) {
        if (currentRepo == null) {
            new Alert(Alert.AlertType.WARNING, "No repository selected.").showAndWait();
            return;
        }

        try {
            // Search for .env file in the repository
            File envFile = findEnvFile(new File(currentRepo.getLocalPath()));
            if (envFile == null) {
                new Alert(Alert.AlertType.WARNING, 
                        ".env file not found in repository:\n" + currentRepo.getLocalPath()).showAndWait();
                return;
            }

            String backupDir = System.getProperty("user.home") + File.separator + ".envvault" + File.separator + "backups";
            File backupDirectory = new File(backupDir);
            if (!backupDirectory.exists()) {
                backupDirectory.mkdirs();
            }

            // Read .env file
            String envContent = new String(Files.readAllBytes(envFile.toPath()));

            // Encrypt the entire .env content
            CryptoEngine.EncryptedPayload payload = CryptoEngine.encryptString(envContent, session.getSecretKey());

            // Create timestamped backup filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String repoName = currentRepo.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String backupFileName = repoName + "_" + timestamp + ".envenc";
            String backupPath = backupDir + File.separator + backupFileName;

            // Write encrypted backup in the format: IV_HEX|CIPHERTEXT_HEX
            String backupContent = VaultEntryRepository.bytesToHex(payload.iv()) + "|" + 
                                  VaultEntryRepository.bytesToHex(payload.ciphertext());
            Files.write(Paths.get(backupPath), backupContent.getBytes(), StandardOpenOption.CREATE_NEW);

            new Alert(Alert.AlertType.INFORMATION, 
                    "✅ Backup created successfully!\n\nLocation: " + backupPath + 
                    "\n\nFound .env at: " + envFile.getAbsolutePath()).showAndWait();
            System.out.println("[Backup] Created encrypted backup: " + backupPath);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Backup failed: " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Searches for .env file in the repository, starting from root
     * Returns the first .env file found, or null if not found
     */
    /**
     * Recursively searches for .env file in the directory and all subdirectories
     */
    private File findEnvFile(File directory) {
        if (!directory.isDirectory()) {
            return null;
        }

        try {
            File[] files = directory.listFiles();
            if (files == null) {
                return null;
            }

            // First pass: check files in current directory
            for (File file : files) {
                if (file.isFile() && file.getName().equals(".env")) {
                    System.out.println("[VaultEditor] Found .env at: " + file.getAbsolutePath());
                    return file;
                }
            }

            // Second pass: recursively search subdirectories (skip hidden and common non-essential folders)
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".") 
                    && !file.getName().equals("node_modules") 
                    && !file.getName().equals("target")
                    && !file.getName().equals(".git")) {
                    
                    File result = findEnvFile(file);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[VaultEditor] Error searching for .env: " + e.getMessage());
        }

        return null;
    }
}


