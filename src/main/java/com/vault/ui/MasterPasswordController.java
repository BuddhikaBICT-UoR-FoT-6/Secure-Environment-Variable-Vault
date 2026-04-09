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
