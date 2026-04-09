package com.vault.ui;

import com.vault.App;
import com.vault.crypto.KeyDerivation;
import com.vault.crypto.SecureMemory;
import com.vault.db.DatabaseManager;
import com.vault.db.VaultMetaRepository;
import com.vault.model.UnlockedVault;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;

/**
 * MasterPasswordController — Handled the initial unlock screen.
 */
public class MasterPasswordController {

    // ── FXML UI Injections ───────────────────────────────────────────────────
    // These variables automatically link to the visual widgets in the .fxml file

    @FXML
    private VBox mainContainer;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private ImageView logoImageView;

    private int logoClickCount = 0;
    private long lastClickTime = 0;
    private static final long CLICK_TIMEOUT_MS = 3000; // Reset counter after 3 seconds

    @FXML
    public void initialize() {
        // Load the generated logo
        try {
            String logoPath = System.getProperty("user.home") + "/.envvault/logo.png";
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                Image logoImage = new Image(logoFile.toURI().toString());
                logoImageView.setImage(logoImage);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
        }

        // Add click handler for logo reset feature
        logoImageView.setOnMouseClicked(event -> handleLogoClick());
        logoImageView.setStyle("-fx-cursor: hand;");
    }

    /**
     * Handles logo clicks for master password reset feature.
     * Clicking 6 times within 3 seconds triggers reset dialog.
     */
    private void handleLogoClick() {
        long currentTime = System.currentTimeMillis();
        
        // Reset counter if too much time has passed
        if (currentTime - lastClickTime > CLICK_TIMEOUT_MS) {
            logoClickCount = 0;
        }
        
        logoClickCount++;
        lastClickTime = currentTime;
        
        System.out.println("[Debug] Logo clicked: " + logoClickCount + " times");
        
        if (logoClickCount >= 6) {
            logoClickCount = 0; // Reset counter
            showResetPasswordDialog();
        }
    }

    /**
     * Shows confirmation dialog and resets the master password.
     */
    private void showResetPasswordDialog() {
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING);
        confirmAlert.setTitle("Reset Master Password");
        confirmAlert.setHeaderText("⚠️ WARNING: Reset Vault?");
        confirmAlert.setContentText(
            "This will DELETE ALL stored data and reset the master password.\n\n" +
            "All repositories and encrypted keys will be permanently lost.\n\n" +
            "This action CANNOT be undone!\n\n" +
            "Do you want to continue?");
        
        confirmAlert.getButtonTypes().setAll(
            javafx.scene.control.ButtonType.YES,
            javafx.scene.control.ButtonType.NO);
        
        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.YES) {
            performVaultReset();
        }
    }

    /**
     * Performs the actual vault reset by clearing all database data.
     */
    private void performVaultReset() {
        try {
            VaultMetaRepository metaRepo = new VaultMetaRepository(
                DatabaseManager.getInstance().getConnection());
            
            // Clear all vault data
            metaRepo.clearAllVaultData();
            
            javafx.scene.control.Alert successAlert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
            successAlert.setTitle("Vault Reset Complete");
            successAlert.setHeaderText("✅ Vault has been reset");
            successAlert.setContentText(
                "All data has been cleared.\n\n" +
                "You can now set a new master password by entering it below.");
            successAlert.showAndWait();
            
            // Clear the password field
            passwordField.clear();
            errorLabel.setVisible(false);
            
            System.out.println("[Reset] Vault reset successful");
            
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            errorAlert.setTitle("Reset Failed");
            errorAlert.setHeaderText("❌ Failed to reset vault");
            errorAlert.setContentText("Error: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * Called automatically by JavaFX when the user clicks the "Unlock" button,
     * or hits the "Enter" key while typing in the password field.
     */
    @FXML
    public void handleUnlock(ActionEvent event) {
        // Clear any previous error messages
        errorLabel.setVisible(false);
        errorLabel.setText("");

        // Get the password characters securely (do not use .getText() which makes
        // immutable Strings if possible,
        // though JavaFX PasswordField internally uses Strings, we do our best below by
        // converting early)
        String passString = passwordField.getText();
        if (passString == null || passString.trim().isEmpty()) {
            showError("Password cannot be empty.");
            return;
        }

        char[] passwordChars = passString.toCharArray();
        VaultMetaRepository metaRepo = new VaultMetaRepository(DatabaseManager.getInstance().getConnection());

        try {
            // 1. Get the stored 16-byte salt (or generate one if this is the very first
            // run)
            String saltHex = metaRepo.getOrGenerateSalt();
            byte[] salt = java.util.HexFormat.of().parseHex(saltHex);

            // 2. Perform PBKDF2 calculation (this takes ~0.3s)
            SecretKey derivedKey = KeyDerivation.deriveKey(passwordChars, salt);

            // 3. (Optional but recommended) In a real production vault, you would try
            // to decrypt a tiny "verification" token stored in the database right now
            // to ensure the password is mathematically correct before letting them in.
            // For our scope, passing them the generated key creates the vault session.
            UnlockedVault session = new UnlockedVault(derivedKey);

            System.out.println("[Login] Master password accepted. Vault unlocked.");

            // 4. Navigate to the Dashboard
            navigateToDashboard(session);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Decryption failed. Wrong password?");
            // Shake animation could go here!
        } finally {
            // ALWAYS wipe out the plaintext password char array from memory immediately
            SecureMemory.wipe(passwordChars);
            passwordField.clear(); // Clear the UI box
        }
    }

    /**
     * Swaps out the current login screen root for the main Dashboard screen.
     */
    private void navigateToDashboard(UnlockedVault session) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("dashboard.fxml"));
            Parent root = loader.load();

            // The Dashboard needs the session to do real work, so we pass it in manually
            DashboardController controller = loader.getController();
            controller.initSession(session);

            // Change the window to the dashboard size (larger)
            App.setRoot(root, 800, 600);

        } catch (IOException e) {
            showError("Failed to load dashboard UI.");
            e.printStackTrace();
        }
    }

    /**
     * Utility to display a red error message to the user.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
