package com.vault.ui;

import com.vault.db.GitRepositoryRepository;
import com.vault.model.GitRepository;
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
 *
 * Allows the user to:
 *   1. Browse to a local cloned git repository directory via a native dialog.
 *   2. Auto-scan the repo for .env keys using EnvKeyScanner.
 *   3. Review and confirm the detected keys before saving to the DB.
 *   4. The linked repo is persisted to the git_repositories table.
 */
public class RepoManagerController {

    @FXML private TextField repoPathField;
    @FXML private TextField repoNameField;
    @FXML private ListView<String> detectedKeysList;
    @FXML private Label statusLabel;
    @FXML private Button linkButton;
    @FXML private Button scanButton;

    private final GitRepositoryRepository repoRepository = new GitRepositoryRepository();

    // Holds the currently detected keys before confirmation
    private Map<String, String> scannedKeys;

    @FXML
    public void initialize() {
        detectedKeysList.setPlaceholder(new Label("Select a repository folder to scan for .env files."));
        statusLabel.setText("");
    }

    /** Opens a native OS directory picker and populates the path field. */
    @FXML
    public void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Local Repository Root");
        File selected = chooser.showDialog(getStage());
        if (selected != null) {
            repoPathField.setText(selected.getAbsolutePath());
            // Auto-populate a name from the folder name
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

        // Run scan on background thread to avoid freezing the UI
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

    /** Saves the linked repository to the database. */
    @FXML
    public void handleLink() {
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
            GitRepository repo = new GitRepository(name, path);
            repoRepository.save(repo);
            setStatus("✅ Repository '" + name + "' linked successfully!", "success");
            linkButton.setDisable(true);
        } catch (Exception e) {
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
