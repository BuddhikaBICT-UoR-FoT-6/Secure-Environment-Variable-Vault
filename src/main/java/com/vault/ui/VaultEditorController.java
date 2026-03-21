package com.vault.ui;

import com.vault.crypto.CryptoEngine;
import com.vault.db.DatabaseManager;
import com.vault.db.VaultEntryRepository;
import com.vault.engine.ProcessInjector;
import com.vault.model.Project;
import com.vault.model.UnlockedVault;
import com.vault.model.VaultEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * VaultEditorController — Manages the secrets for a specific project.
 */
public class VaultEditorController {

    @FXML
    private VBox editorContainer;
    @FXML
    private Label projectTitleLabel;

    // The table that lists the environment variables
    @FXML
    private TableView<VaultEntry> secretsTable;
    @FXML
    private TableColumn<VaultEntry, String> keyColumn;
    @FXML
    private TableColumn<VaultEntry, String> valueColumn;

    private Project currentProject;
    private UnlockedVault session;
    private VaultEntryRepository vaultRepo;
    private ObservableList<VaultEntry> entriesModel;

    @FXML
    public void initialize() {
        vaultRepo = new VaultEntryRepository(DatabaseManager.getInstance().getConnection());
        entriesModel = FXCollections.observableArrayList();

        // Tell the table how to extract data from the VaultEntry object
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("keyName"));

        // Custom cell factory for the value column so we can mask the secrets
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("decryptedValue"));
        valueColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Show a masked version unless they explicitly click "Reveal"
                    setText("••••••••••••••");
                }
            }
        });

        secretsTable.setItems(entriesModel);
    }

    /**
     * Called by DashboardController immediately after loading this view.
     */
    public void initData(Project project, UnlockedVault session) {
        this.currentProject = project;
        this.session = session;
        projectTitleLabel.setText("Secrets for: " + project.getName());
        loadSecrets();
    }

    private void loadSecrets() {
        entriesModel.clear();
        List<VaultEntry> encryptedEntries = vaultRepo.listEntriesForProject(currentProject.getId());

        // We eagerly decrypt them into memory so they can be viewed/launched easily.
        // If the vault locks, the session object is destroyed anyway.
        for (VaultEntry entry : encryptedEntries) {
            try {
                CryptoEngine.EncryptedPayload payload = new CryptoEngine.EncryptedPayload(
                        VaultEntryRepository.hexToBytes(entry.getIvHex()),
                        VaultEntryRepository.hexToBytes(entry.getCiphertextHex()));

                String decrypted = CryptoEngine.decryptString(payload, session.getSecretKey());
                entry.setDecryptedValue(decrypted);
                entriesModel.add(entry);

            } catch (Exception e) {
                System.err.println("Failed to decrypt key: " + entry.getKeyName());
            }
        }
    }

    @FXML
    public void handleAddSecret(ActionEvent event) {
        // We need a custom dialog to capture BOTH key and value.
        // For simplicity here, we'll use a hacky text dialog separating them by '='
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
                    // Encrypt the value
                    CryptoEngine.EncryptedPayload payload = CryptoEngine.encryptString(value, session.getSecretKey());

                    // Create the database record
                    VaultEntry newEntry = new VaultEntry(
                            currentProject.getId(),
                            key,
                            VaultEntryRepository.bytesToHex(payload.iv()),
                            VaultEntryRepository.bytesToHex(payload.ciphertext()));

                    // Save to DB
                    vaultRepo.saveEntry(newEntry);

                    // Reload table
                    loadSecrets();

                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    @FXML
    public void handleLaunch(ActionEvent event) {
        if (currentProject.getLaunchCommand() == null || currentProject.getWorkingDirectory() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Please configure the Launch Command and Working Directory first!");
            alert.showAndWait();
            return;
        }

        try {
            ProcessInjector injector = new ProcessInjector();

            // This is where the magic happens! We pass the list of encrypted entries
            // from the DB, and the active session key. It handles the injection.
            Process p = injector.launch(
                    currentProject.getLaunchCommand(),
                    currentProject.getWorkingDirectory(),
                    vaultRepo.listEntriesForProject(currentProject.getId()), // pass fresh from db
                    session.getSecretKey());

            // TODO: In a fully polished app, we would open a new dialogue panel
            // and pass 'p.getInputStream()' into ProcessOutputStreamer to see the logs.
            System.out.println("[App] Process launched successfully in background.");

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to launch process: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
