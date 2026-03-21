package com.vault.ui;

import com.vault.App;
import com.vault.db.DatabaseManager;
import com.vault.db.ProjectRepository;
import com.vault.model.Project;
import com.vault.model.UnlockedVault;
import com.vault.ui.VaultEditorController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;

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
    private ObservableList<Project> projectListModel;

    /**
     * Called automatically by JavaFX when the FXML file is loaded.
     * We initialize the database repository and the UI list here.
     */
    @FXML
    public void initialize() {
        projectRepo = new ProjectRepository(DatabaseManager.getInstance().getConnection());
        projectListModel = FXCollections.observableArrayList();
        projectListView.setItems(projectListModel);

        // Load all existing projects into the sidebar
        refreshProjectList();

        // Add a listener to detect when the user clicks a project in the sidebar
        projectListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadProjectEditor(newValue);
                    }
                });
    }

    /**
     * Accepts the unlocked session from the MasterPasswordController.
     * This session token guarantees we are securely unlocked and have the AES key.
     */
    public void initSession(UnlockedVault session) {
        this.session = session;
    }

    // ── UI Actions ───────────────────────────────────────────────────────────

    @FXML
    public void handleNewProject(ActionEvent event) {
        // Native JavaFX popup to prompt for a project name
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

                // Auto-select the newly created project
                projectListView.getSelectionModel().select(newProject);
            }
        });
    }

    @FXML
    public void handleDeleteProject(ActionEvent event) {
        Project selected = projectListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Native JavaFX confirmation alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Project");
        alert.setHeaderText("Delete " + selected.getName() + " and all its secrets?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            projectRepo.deleteProject(selected.getId());
            refreshProjectList();

            // Clear the center area since the project is gone
            rootPane.setCenter(null);
        }
    }

    @FXML
    public void handleLockVault(ActionEvent event) {
        if (session != null) {
            session.lock(); // destroys the key in memory!
        }

        // Return to login screen
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("master_password.fxml"));
            Parent root = loader.load();
            App.setRoot(root, 400, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private void refreshProjectList() {
        projectListModel.setAll(projectRepo.listProjects());
    }

    /**
     * Loads the Vault Editor (the table of secrets) into the center of the UI
     * when a project is selected in the sidebar.
     */
    private void loadProjectEditor(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("vault_editor.fxml"));
            Parent editorView = loader.load();

            VaultEditorController controller = loader.getController();
            // Pass the selected project AND the secure session into the editor
            controller.initData(project, session);

            // Put the editor panel directly in the middle of our dashboard!
            rootPane.setCenter(editorView);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load vault_editor.fxml. Have you created it yet?");
        }
    }
}
