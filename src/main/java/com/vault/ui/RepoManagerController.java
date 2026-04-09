package com.vault.ui;

import com.vault.db.DatabaseManager;
import com.vault.db.GitRepositoryRepository;
import com.vault.db.VaultEntryRepository;
import com.vault.model.GitRepository;
import com.vault.model.UnlockedVault;
import com.vault.model.VaultEntry;
import com.vault.scanner.EnvKeyScanner;
import com.vault.util.EnvFileLocker;
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
 * Works directly with repositories without project hierarchy.
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

    private UnlockedVault session;
    private Runnable onFinish;

    // Holds the currently detected keys before confirmation
    private Map<String, String> scannedKeys;
    private String envFilePathToLock;

    @FXML
    public void initialize() {
        detectedKeysList.setPlaceholder(new Label("Select a repository folder to scan for .env files."));
        statusLabel.setText("");
    }

    /** Sets up the controller with vault session. */
    public void setup(UnlockedVault session, Runnable onFinish) {
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
            
            // Find the .env file path for locking
            File[] envFiles = new File(path).listFiles((d, n) -> n.equals(".env"));
            if (envFiles != null && envFiles.length > 0) {
                envFilePathToLock = envFiles[0].getAbsolutePath();
            }

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

    /** Saves the linked repository and its keys to the database, then locks the .env file. */
    @FXML
    public void handleLink() {
        if (session == null) {
            setStatus("❌ Error: No active vault session.", "error");
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
            // 1. Save the Git Repository metadata (no projectId needed)
            GitRepository repo = new GitRepository(name, path);
            repo.setProjectId(-1); // No project - standalone repo
            repoRepository.save(repo);

            // 2. Encrypt and save all discovered keys
            for (Map.Entry<String, String> entry : scannedKeys.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                VaultEntry vaultEntry = session.encryptEntry(key, value);
                vaultEntry.setProjectId(repo.getId()); // Link to repo ID directly
                vaultRepo.saveEntry(vaultEntry);
            }

            // 3. Lock the .env file
            if (envFilePathToLock != null && !envFilePathToLock.isEmpty()) {
                boolean locked = EnvFileLocker.lockEnvFile(envFilePathToLock);
                if (locked) {
                    setStatus("✅ Repository linked, keys encrypted & .env locked!", "success");
                } else {
                    setStatus("⚠ Repository linked but failed to lock .env file.", "warning");
                }
            } else {
                setStatus("✅ Repository linked and " + scannedKeys.size() + " keys added!", "success");
            }

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
