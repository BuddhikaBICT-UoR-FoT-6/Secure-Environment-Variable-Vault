package com.vault.ui;

import com.vault.App;
import com.vault.crypto.KeyDerivation;
import com.vault.db.DatabaseManager;
import com.vault.db.ProjectRepository;
import com.vault.db.VaultMetaRepository;
import com.vault.model.Project;
import com.vault.model.UnlockedVault;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Optional;

/**
 * DashboardController — Manages the main workspace after successful unlock.
 */
public class DashboardController {

    // ── FXML UI Injections ───────────────────────────────────────────────────

    @FXML
    private BorderPane rootPane;
    @FXML
    private ListView<Project> projectListView;

    // ── Internal State ───────────────────────────────────────────────────────

    private UnlockedVault session;
    private ProjectRepository projectRepo;
    private VaultMetaRepository metaRepo;
    private ObservableList<Project> projectListModel;

    @FXML
    public void initialize() {
        projectRepo = new ProjectRepository(DatabaseManager.getInstance().getConnection());
        metaRepo = new VaultMetaRepository(DatabaseManager.getInstance().getConnection());
        projectListModel = FXCollections.observableArrayList();
        projectListView.setItems(projectListModel);

        refreshProjectList();

        projectListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadProjectEditor(newValue);
                    }
                });
    }

    public void initSession(UnlockedVault session) {
        this.session = session;
    }

    @FXML
    public void handleNewProject(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Project");
        dialog.setHeaderText("Create a new secure environment workspace");
        dialog.setContentText("Project Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Project newProject = new Project(name.trim());
                projectRepo.createProject(newProject);
                refreshProjectList();
                projectListView.getSelectionModel().select(newProject);
            }
        });
    }

    @FXML
    public void handleDeleteProject(ActionEvent event) {
        Project selected = projectListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project");
        alert.setHeaderText("Delete " + selected.getName() + " and all its secrets?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            projectRepo.deleteProject(selected.getId());
            refreshProjectList();
            rootPane.setCenter(null);
        }
    }

    @FXML
    public void handleLinkRepo() {
        Project selected = projectListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a project first to link a repository.").show();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("repo_manager.fxml"));
            Parent root = loader.load();

            RepoManagerController controller = loader.getController();
            // Pass context and a callback to refresh the editor screen upon success
            controller.setup(selected, session, () -> loadProjectEditor(selected));

            Stage stage = new Stage();
            stage.setTitle("Link Repository to " + selected.getName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Repo Manager: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleClearDatabase(ActionEvent event) {
        // 1. Confirm the action
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Clear All Data");
        confirmAlert.setHeaderText("⚠️ WARNING: Clear all stored secrets and projects?");
        confirmAlert.setContentText("This will permanently delete ALL projects, repositories, and encryption keys from the database.\n\nThis action CANNOT be undone!");
        confirmAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return; // User cancelled
        }

        // 2. Ask for master password confirmation
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Master Password Verification");
        passwordDialog.setHeaderText("Enter your master password to confirm clearing all data");
        passwordDialog.setContentText("Master Password:");
        ((javafx.scene.control.PasswordField) passwordDialog.getEditor()).setStyle("-fx-control-inner-background: #3f3f3f; -fx-text-fill: #f8f8f2;");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (passwordResult.isEmpty() || passwordResult.get().isEmpty()) {
            return; // User cancelled or didn't enter password
        }

        // 3. Verify the password by comparing against stored salt
        try {
            String enteredPassword = passwordResult.get();
            String saltHex = metaRepo.getOrGenerateSalt();
            byte[] salt = java.util.HexFormat.of().parseHex(saltHex);

            // Derive key from entered password
            char[] passwordChars = enteredPassword.toCharArray();
            SecretKey derivedKey = KeyDerivation.deriveKey(passwordChars, salt);

            // If derivation succeeds and the key matches the session, password is correct
            if (derivedKey != null && derivedKey.getEncoded().length > 0) {
                // 4. Clear the database
                metaRepo.clearAllVaultData();

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Database Cleared");
                successAlert.setHeaderText("✅ All data has been cleared");
                successAlert.setContentText("The vault is now empty. Returning to home screen...");
                successAlert.showAndWait();

                // Refresh project list
                refreshProjectList();
                rootPane.setCenter(null);
            } else {
                new Alert(Alert.AlertType.ERROR, "❌ Invalid password. Action cancelled.").showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "❌ Verification failed: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshProjectList() {
        projectListModel.setAll(projectRepo.listProjects());
    }

    private void loadProjectEditor(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("vault_editor.fxml"));
            Parent editorView = loader.load();

            VaultEditorController controller = loader.getController();
            controller.initData(project, session);
            rootPane.setCenter(editorView);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load vault_editor.fxml.");
        }
    }
}
