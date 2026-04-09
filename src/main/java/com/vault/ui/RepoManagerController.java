package com.vault.ui;

import com.vault.db.DatabaseManager;
import com.vault.db.GitRepositoryRepository;
import com.vault.db.VaultEntryRepository;
import com.vault.model.GitRepository;
import com.vault.model.Project;
import com.vault.model.UnlockedVault;
import com.vault.model.VaultEntry;
import com.vault.scanner.EnvKeyScanner;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * RepoManagerController — handles the "Link Repository" screen.
 */
public class RepoManagerController {

    @FXML private TextField repoPathField;
    @FXML private TextField repoNameField;
    @FXML private ListView<String> detectedKeysList;
    @FXML private Label statusLabel;
    @FXML private Button linkButton;
    @FXML private Button scanButton;

    private final GitRepositoryRepository repoRepository = new GitRepositoryRepository();
    private final VaultEntryRepository vaultRepo = new VaultEntryRepository(DatabaseManager.getInstance().getConnection());

    private Project currentProject;
    private UnlockedVault session;
    private Runnable onFinish;

    // Holds the currently detected keys before confirmation
    private Map<String, String> scannedKeys;

    @FXML
    public void initialize() {
        detectedKeysList.setPlaceholder(new Label("Select a repository folder to scan for .env files."));
        statusLabel.setText("");
    }

    /** Sets up the controller with necessary project context. */
    public void setup(Project project, UnlockedVault session, Runnable onFinish) {
        this.currentProject = project;
        this.session = session;
        this.onFinish = onFinish;
    }

    /** Opens a native OS directory picker and populates the path field. */
    @FXML
    public void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Local Repository Root");
        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            repoPathField.setText(selected.getAbsolutePath());
            if (repoNameField.getText().isBlank()) {
                repoNameField.setText(selected.getName());
            }
        }
    }

    /** Runs EnvKeyScanner on the given path and shows discovered keys. */
    @FXML
    public void handleScan() {
        String path = repoPathField.getText().trim();
        if (path.isBlank()) {
            setStatus("⚠ Please select a repository folder first.", "warning");
            return;
        }
        if (!new File(path).isDirectory()) {
            setStatus("⚠ Path does not exist or is not a directory.", "warning");
            return;
        }

        setStatus("🔍 Scanning for .env files...", "info");
        scanButton.setDisable(true);

        new Thread(() -> {
            EnvKeyScanner scanner = new EnvKeyScanner(path);
            scannedKeys = scanner.scan();
            List<String> keyList = List.copyOf(scannedKeys.keySet());

            Platform.runLater(() -> {
                detectedKeysList.setItems(FXCollections.observableArrayList(keyList));
                if (keyList.isEmpty()) {
                    setStatus("No .env keys found in this repository.", "warning");
                } else {
                    setStatus("✅ Found " + keyList.size() + " keys. Review and click Link.", "success");
                }
                scanButton.setDisable(false);
            });
        }, "RepoScanner").start();
    }

    /** Saves the linked repository and its keys to the database. */
    @FXML
    public void handleLink() {
        if (currentProject == null || session == null) {
            setStatus("❌ Error: No active project context.", "error");
            return;
        }

        String path = repoPathField.getText().trim();
        String name = repoNameField.getText().trim();

        if (path.isBlank() || name.isBlank()) {
            setStatus("⚠ Please provide both a name and a path.", "warning");
            return;
        }
        if (scannedKeys == null || scannedKeys.isEmpty()) {
            setStatus("⚠ Please scan the repo first.", "warning");
            return;
        }

        try {
            // 1. Save the Git Repository metadata
            GitRepository repo = new GitRepository(name, path);
            repo.setProjectId(currentProject.getId());
            repoRepository.save(repo);

            // 2. Encrypt and save all discovered keys
            for (Map.Entry<String, String> entry : scannedKeys.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                VaultEntry vaultEntry = session.encryptEntry(key, value);
                vaultEntry.setProjectId(currentProject.getId());
                vaultRepo.saveEntry(vaultEntry);
            }

            setStatus("✅ Repository linked and " + scannedKeys.size() + " keys added!", "success");
            linkButton.setDisable(true);

            // Wait a moment so user sees the success message, then close and refresh
            new Thread(() -> {
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    if (onFinish != null) onFinish.run();
                    getStage().close();
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("❌ Error: " + e.getMessage(), "error");
        }
    }

    @FXML
    public void handleCancel() {
        getStage().close();
    }

    private void setStatus(String message, String type) {
        statusLabel.setText(message);
        statusLabel.setStyle(switch (type) {
            case "success" -> "-fx-text-fill: #50fa7b;";
            case "warning" -> "-fx-text-fill: #ffb86c;";
            case "error"   -> "-fx-text-fill: #ff5555;";
            default        -> "-fx-text-fill: #8be9fd;";
        });
    }

    private Stage getStage() {
        return (Stage) repoPathField.getScene().getWindow();
    }
}
