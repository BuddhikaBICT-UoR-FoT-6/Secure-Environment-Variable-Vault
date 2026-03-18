package com.vault;

import com.vault.db.DatabaseManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * App — The main entry point for the JavaFX Application.
 *
 * Every JavaFX application must:
 * 1. Extend javafx.application.Application
 * 2. Override the start() method
 * 3. Have a main() method that calls launch()
 */
public class App extends Application {

    /**
     * A global reference to the main window (Stage).
     * We keep this static so controllers can easily find the main window
     * to change scenes (e.g., login screen -> dashboard).
     */
    private static Stage primaryStage;

    /**
     * start() is called automatically by JavaFX AFTER the GUI toolkit has
     * initialised.
     * This runs on the special "JavaFX Application Thread".
     *
     * @param stage The primary OS window provided by JavaFX
     */
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Force the DatabaseManager to wake up and connect to SQLite immediately.
        // If this fails (e.g., file permissions), the app crashes here cleanly,
        // rather than half-loading the UI and dying mysteriously later.
        DatabaseManager.getInstance();

        // Load the initial FXML layout (the Master Password unlock screen)
        // Note: The path expects the FXML to exist in src/main/resources/com/vault/
        FXMLLoader loader = new FXMLLoader(getClass().getResource("master_password.fxml"));
        Parent root = loader.load();

        // Create the scene (the "canvas" inside the window)
        Scene scene = new Scene(root, 400, 300);

        // Load our custom Dracula-inspired dark theme
        String css = getClass().getResource("dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        // Setup the physical OS window properties
        stage.setTitle("Local Env Vault");
        stage.setScene(scene);
        stage.setResizable(false); // keep the login screen locked to 400x300
        stage.show();
    }

    /**
     * stop() is called automatically when the user closes the app (clicks the red
     * X).
     * This is the perfect place to clean up resources, close database connections,
     * and kill background watcher threads.
     */
    @Override
    public void stop() throws Exception {
        System.out.println("[App] Shutting down...");
        DatabaseManager.getInstance().close();
        System.out.println("[App] Goodbye!");
    }

    /**
     * A simple helper method so that controllers can trigger a screen change.
     * Often used to swap from internal panels back to the login screen after
     * inactivity.
     *
     * @param root   The new UI layout to display
     * @param width  The window width
     * @param height The window height
     */
    public static void setRoot(Parent root, int width, int height) {
        Scene scene = primaryStage.getScene();
        scene.setRoot(root);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);

        // Re-center the window on the screen if the size drastically changed
        primaryStage.centerOnScreen();
    }

    /**
     * The standard JVM entry point.
     * launch() is a static method in Application that kicks off the JavaFX
     * lifecycle.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
