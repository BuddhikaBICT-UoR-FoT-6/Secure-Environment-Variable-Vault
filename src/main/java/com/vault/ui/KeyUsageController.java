package com.vault.ui;

import com.vault.scanner.KeyUsageResult;
import com.vault.scanner.KeyUsageTracker;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

/**
 * KeyUsageController — displays all source files that reference a specific
 * environment variable key within the linked repository.
 *
 * Populated by the VaultEditorController when the user clicks "View Usage".
 * Shows a read-only table: [File Path | Line # | Code Snippet]
 */
public class KeyUsageController {

    @FXML private Label keyNameLabel;
    @FXML private Label repoPathLabel;
    @FXML private TableView<KeyUsageResult> usageTable;
    @FXML private TableColumn<KeyUsageResult, String> fileColumn;
    @FXML private TableColumn<KeyUsageResult, Integer> lineColumn;
    @FXML private TableColumn<KeyUsageResult, String> snippetColumn;
    @FXML private Label resultCountLabel;

    @FXML
    public void initialize() {
        fileColumn.setCellValueFactory(new PropertyValueFactory<>("relativeFilePath"));
        lineColumn.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        snippetColumn.setCellValueFactory(new PropertyValueFactory<>("lineContent"));

        fileColumn.setPrefWidth(280);
        lineColumn.setPrefWidth(80);
        snippetColumn.setPrefWidth(350);

        usageTable.setPlaceholder(new Label("No usages found in this repository."));
    }

    /**
     * Called by VaultEditorController to populate this screen before opening.
     *
     * @param keyName      the env variable key name to search for
     * @param repoRootPath absolute path to the linked repository
     */
    public void load(String keyName, String repoRootPath) {
        keyNameLabel.setText("Usages of: " + keyName);
        repoPathLabel.setText("Repository: " + repoRootPath);
        resultCountLabel.setText("Scanning...");

        // Run scan on background thread
        new Thread(() -> {
            KeyUsageTracker tracker = new KeyUsageTracker(repoRootPath);
            List<KeyUsageResult> results = tracker.findUsages(keyName);

            javafx.application.Platform.runLater(() -> {
                usageTable.setItems(FXCollections.observableArrayList(results));
                resultCountLabel.setText(results.size() + " occurrence(s) found.");
            });
        }, "KeyUsageScanner-" + keyName).start();
    }

    @FXML
    public void handleClose() {
        ((Stage) usageTable.getScene().getWindow()).close();
    }
}
