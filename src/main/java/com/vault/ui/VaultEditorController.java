package com.vault.ui;

import com.vault.crypto.CryptoEngine;
import com.vault.db.DatabaseManager;
import com.vault.db.VaultEntryRepository;
import com.vault.db.GitRepositoryRepository;
import com.vault.model.UnlockedVault;
import com.vault.model.VaultEntry;
import com.vault.model.GitRepository;
import com.vault.security.FaceAuthManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

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
    @FXML
    private TableColumn<VaultEntry, Boolean> lockColumn;
    @FXML
    private TableColumn<VaultEntry, Void> actionsColumn;

    private GitRepository currentRepo;
    private UnlockedVault session;
    private VaultEntryRepository vaultRepo;
    private ObservableList<VaultEntry> entriesModel;

    private final GitRepositoryRepository repoRepo = new GitRepositoryRepository();
    private final FaceAuthManager faceManager = new FaceAuthManager();

    // Cache to track which keys are "temporarily revealed" in this session
    private final Map<Integer, Boolean> revealedStates = new HashMap<>();

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
                    setGraphic(null);
                } else {
                    VaultEntry entry = getTableView().getItems().get(getIndex());
                    if (entry.isLocked() && !revealedStates.getOrDefault(entry.getId(), false)) {
                        setText("•••• [LOCKED] ••••");
                        setGraphic(null);
                    } else if (revealedStates.getOrDefault(entry.getId(), false)) {
                        setText(item);
                        setGraphic(null);
                    } else {
                        setText("••••••••••••••");
                        Button revealBtn = new Button("👁");
                        revealBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8be9fd; -fx-cursor: hand;");
                        revealBtn.setOnAction(e -> {
                            revealedStates.put(entry.getId(), true);
                            getTableView().refresh();
                        });
                        setGraphic(revealBtn);
                    }
                }
            }
        });

        lockColumn.setCellValueFactory(new PropertyValueFactory<>("locked"));
        lockColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean locked, boolean empty) {
                super.updateItem(locked, empty);
                if (empty || locked == null) {
                    setText(null);
                } else {
                    setText(locked ? "🔒" : "🔓");
                }
            }
        });

        actionsColumn.setCellFactory(tc -> new TableCell<>() {
            private final Button usageBtn = new Button("📊 Usage");
            private final HBox container = new HBox(5, usageBtn);

            {
                usageBtn.setStyle("-fx-font-size: 10;");
                usageBtn.setOnAction(e -> handleViewUsage(getTableView().getItems().get(getIndex())));
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
            return;
        }

        List<VaultEntry> encryptedEntries = vaultRepo.listEntriesForProject(currentRepo.getId());

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
                    CryptoEngine.EncryptedPayload payload = CryptoEngine.encryptString(value, session.getSecretKey());
                    VaultEntry newEntry = new VaultEntry(
                            currentRepo.getId(),
                            key,
                            VaultEntryRepository.bytesToHex(payload.iv()),
                            VaultEntryRepository.bytesToHex(payload.ciphertext()));

                    vaultRepo.saveEntry(newEntry);
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

    private void handleViewUsage(VaultEntry entry) {
        List<GitRepository> repos = repoRepo.findAll();
        if (repos.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No repositories linked. Click 'Link Repo' on the dashboard first.");
            alert.showAndWait();
            return;
        }

        GitRepository repo = repos.get(0);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("key_usage.fxml"));
            Parent root = loader.load();
            KeyUsageController ctrl = loader.getController();
            ctrl.load(entry.getKeyName(), repo.getLocalPath());

            Stage stage = new Stage();
            stage.setTitle("Key Usage Tracker");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(secretsTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lock management removed - lock/unlock functionality is built-in via locking at display time
    // and no longer settable through the UI per user request
}

