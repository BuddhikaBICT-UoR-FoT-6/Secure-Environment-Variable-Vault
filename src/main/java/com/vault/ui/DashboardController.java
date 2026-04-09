package com.vault.ui;

import com.vault.App;
import com.vault.db.DatabaseManager;
import com.vault.db.GitRepositoryRepository;
import com.vault.db.VaultMetaRepository;
import com.vault.model.GitRepository;
import com.vault.model.UnlockedVault;
import com.vault.crypto.KeyDerivation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Optional;

/**
 * DashboardController — Manages the main repository management interface.
 * No projects - only direct repository management with encrypted keys.
 */
public class DashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private ListView<GitRepository> repoListView;

    private UnlockedVault session;
    private GitRepositoryRepository repoRepo;
    private VaultMetaRepository metaRepo;
    private ObservableList<GitRepository> repoListModel;

    @FXML
    public void initialize() {
        repoRepo = new GitRepositoryRepository();
        metaRepo = new VaultMetaRepository(DatabaseManager.getInstance().getConnection());
        repoListModel = FXCollections.observableArrayList();
        repoListView.setItems(repoListModel);

        // Display repository names nicely in the ListView
        repoListView.setCellFactory(lv -> new ListCell<GitRepository>() {
            @Override
            protected void updateItem(GitRepository repo, boolean empty) {
                super.updateItem(repo, empty);
                setText(empty || repo == null ? "" : repo.getName());
            }
        });

        refreshRepoList();

        repoListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadRepoEditor(newValue);
                    }
                });
    }

    public void initSession(UnlockedVault session) {
        this.session = session;
    }

    @FXML
    public void handleLinkRepo() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("repo_manager.fxml"));
            Parent root = loader.load();

            RepoManagerController controller = loader.getController();
            controller.setup(session, this::refreshRepoList);

            Stage stage = new Stage();
            stage.setTitle("Link Repository");
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
    public void handleDeleteRepo() {
        GitRepository selected = repoListView.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Repository");
        alert.setHeaderText("Remove " + selected.getName() + " from vault?");
        alert.setContentText("The repository will be forgotten but .env file access is restored.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            repoRepo.delete(selected.getId());
            refreshRepoList();
            rootPane.setCenter(null);
        }
    }

    @FXML
    public void handleClearDatabase(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Clear All Data");
        confirmAlert.setHeaderText("⚠️ WARNING: Clear all stored secrets?");
        confirmAlert.setContentText("This will permanently delete ALL repositories and encryption keys from the database.\n\nThis action CANNOT be undone!");
        confirmAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Master Password Verification");
        passwordDialog.setHeaderText("Enter your master password to confirm");
        passwordDialog.setContentText("Master Password:");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (passwordResult.isEmpty() || passwordResult.get().isEmpty()) {
            return;
        }

        try {
            String enteredPassword = passwordResult.get();
            String saltHex = metaRepo.getOrGenerateSalt();
            byte[] salt = java.util.HexFormat.of().parseHex(saltHex);
            char[] passwordChars = enteredPassword.toCharArray();
            SecretKey derivedKey = KeyDerivation.deriveKey(passwordChars, salt);

            if (derivedKey != null && derivedKey.getEncoded().length > 0) {
                metaRepo.clearAllVaultData();

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Database Cleared");
                successAlert.setHeaderText("✅ All data has been cleared");
                successAlert.setContentText("The vault is now empty. Returning to home screen...");
                successAlert.showAndWait();

                refreshRepoList();
                rootPane.setCenter(null);
            } else {
                new Alert(Alert.AlertType.ERROR, "❌ Invalid password. Action cancelled.").showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "❌ Verification failed: " + e.getMessage()).showAndWait();
        }
    }

    private void refreshRepoList() {
        repoListModel.setAll(repoRepo.findAll());
    }

    private void loadRepoEditor(GitRepository repo) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("vault_editor.fxml"));
            Parent editor = loader.load();

            VaultEditorController ctrl = loader.getController();
            ctrl.initDataForRepo(repo, session);

            rootPane.setCenter(editor);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load editor: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
