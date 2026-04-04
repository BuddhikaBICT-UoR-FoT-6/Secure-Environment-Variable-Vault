package com.vault.ui;

import com.vault.security.FaceAuthManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * FaceLockController — manages the biometric (face) and fallback (grid-PIN) unlock UI.
 *
 * Scenarios:
 *   1. User attempts to view a locked key -> Face scan prompt appears.
 *   2. If webcam fails or user clicks "Use PIN" -> A shuffled grid of characters
 *      appears for alphanumeric PIN entry.
 */
public class FaceLockController {

    @FXML private Label statusLabel;
    @FXML private Label keyLabel;
    @FXML private Button unlockButton;
    @FXML private Button pinButton;
    @FXML private GridPane pinGrid;
    @FXML private PasswordField pinField;

    private String targetKey;
    private String savedFacePath;
    private String savedPinHash;
    private Consumer<Boolean> onResult;

    private final FaceAuthManager faceManager = new FaceAuthManager();
    private final StringBuilder currentPin = new StringBuilder();

    @FXML
    public void initialize() {
        pinGrid.setVisible(false);
        pinField.setVisible(false);
    }

    /**
     * Initializes the unlock session.
     * @param keyName   The name of the key being unlocked
     * @param facePath  Path to the enrolled face image (if any)
     * @param pinHash   SHA-256 hash of the set PIN (fallback)
     * @param onResult  Callback to return the success/failure result
     */
    public void setup(String keyName, String facePath, String pinHash, Consumer<Boolean> onResult) {
        this.targetKey = keyName;
        this.savedFacePath = facePath;
        this.savedPinHash = pinHash;
        this.onResult = onResult;
        keyLabel.setText("Unlocking Key: " + keyName);
    }

    @FXML
    public void handleFaceUnlock() {
        statusLabel.setText("🔍 Scanning... Please look at the camera.");
        unlockButton.setDisable(true);

        new Thread(() -> {
            boolean success = faceManager.verifyFace(savedFacePath);
            Platform.runLater(() -> {
                if (success) {
                    statusLabel.setText("✅ Face Verified!");
                    onResult.accept(true);
                    close();
                } else {
                    statusLabel.setText("❌ Verification Failed. Try again or use PIN.");
                    unlockButton.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    public void handlePinMode() {
        pinGrid.setVisible(true);
        pinField.setVisible(true);
        unlockButton.setVisible(false);
        pinButton.setVisible(false);
        statusLabel.setText("Select your PIN characters in order:");
        
        setupPinGrid();
    }

    private void setupPinGrid() {
        pinGrid.getChildren().clear();
        List<String> chars = new ArrayList<>();
        // Add A-Z, 0-9 for simplicity in the grid
        for (char c = 'A'; c <= 'Z'; c++) chars.add(String.valueOf(c));
        for (char c = '0'; c <= '9'; c++) chars.add(String.valueOf(c));
        
        Collections.shuffle(chars);

        int row = 0;
        int col = 0;
        for (String s : chars) {
            Button btn = new Button(s);
            btn.setPrefWidth(40);
            btn.setOnAction(e -> appendChar(s));
            pinGrid.add(btn, col++, row);
            if (col > 5) {
                col = 0;
                row++;
            }
        }
    }

    private void appendChar(String s) {
        currentPin.append(s);
        pinField.setText(currentPin.toString());
    }

    @FXML
    public void handlePinSubmit() {
        if (faceManager.verifyPin(currentPin.toString(), savedPinHash)) {
            statusLabel.setText("✅ PIN Accepted!");
            onResult.accept(true);
            close();
        } else {
            statusLabel.setText("❌ Incorrect PIN. Cleared.");
            currentPin.setLength(0);
            pinField.clear();
        }
    }

    @FXML
    public void handleClearPin() {
        currentPin.setLength(0);
        pinField.clear();
    }

    @FXML
    public void handleCancel() {
        onResult.accept(false);
        close();
    }

    private void close() {
        ((Stage) statusLabel.getScene().getWindow()).close();
    }
}
