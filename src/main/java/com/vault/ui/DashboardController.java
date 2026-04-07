package com.vault.ui;

import com.vault.App;
import com.vault.db.DatabaseManager;
import com.vault.db.ProjectRepository;
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

    @FXML
    public void initialize() {
        projectRepo = new ProjectRepository(DatabaseManager.getInstance().getConnection());
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
    public void handleLockVault(ActionEvent event) {
        if (session != null) {
            session.lock();
        }
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("master_password.fxml"));
            Parent root = loader.load();
            App.setRoot(root, 400, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLinkRepo() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("repo_manager.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Manage Linked Repositories");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load Repo Manager: " + e.getMessage());
            alert.showAndWait();
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
