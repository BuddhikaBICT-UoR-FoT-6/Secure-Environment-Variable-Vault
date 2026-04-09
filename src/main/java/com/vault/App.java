package com.vault;

import com.vault.db.DatabaseManager;
import com.vault.util.EnvFileLocker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * App — The main entry point for the JavaFX Application.
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Initialize database
        DatabaseManager.getInstance();

        // Generate custom logo if it doesn't exist
        String logoPath = System.getProperty("user.home") + "/.envvault/logo.png";
        generateAppLogo(logoPath);

        // Set Application Icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("logo.png")));
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        // Load the initial FXML layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("master_password.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 400, 320); // slightly taller for logo

        // Load custom styles
        String css = getClass().getResource("dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Local Env Vault");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Generates a 128x128 PNG logo with an artistic vault/shield design.
     * Saves to user home directory.
     */
    private static void generateAppLogo(String outputPath) {
        File logoFile = new File(outputPath);
        
        try {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // High-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Transparent background
            g2d.clearRect(0, 0, 128, 128);

            // Create gradient for depth
            int cx = 64, cy = 64;
            
            // Main shield shape (simplified vault)
            int[] shieldX = {cx - 35, cx - 35, cx, cx + 35, cx + 35, cx};
            int[] shieldY = {cy - 30, cy - 10, cy + 38, cy - 10, cy - 30, cy - 35};
            
            // Shield background - gradient purple to darker purple
            Paint gradientPaint = new java.awt.GradientPaint(cx, cy - 30, new Color(189, 147, 249, 240), 
                                                              cx, cy + 35, new Color(150, 100, 210, 255));
            g2d.setPaint(gradientPaint);
            g2d.fillPolygon(shieldX, shieldY, 6);

            // Shield border - darker purple
            g2d.setColor(new Color(120, 80, 160, 255));
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawPolygon(shieldX, shieldY, 6);

            // Inner shield highlight - subtle
            int[] innerX = {cx - 32, cx - 32, cx, cx + 32, cx + 32, cx};
            int[] innerY = {cy - 28, cy - 8, cy + 35, cy - 8, cy - 28, cy - 33};
            g2d.setColor(new Color(220, 180, 255, 60));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawPolygon(innerX, innerY, 6);

            // Artistic lock icon in center - simplified design
            // Lock shackle (arc) - cyan
            g2d.setColor(new Color(139, 233, 253, 255)); // #8be9fd
            g2d.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawArc(cx - 16, cy - 18, 32, 32, 0, 180);

            // Lock body - solid cyan
            int lockBodyX = cx - 18;
            int lockBodyY = cy + 2;
            int lockBodyW = 36;
            int lockBodyH = 24;
            
            // Rounded lock body
            g2d.fillRoundRect(lockBodyX, lockBodyY, lockBodyW, lockBodyH, 6, 6);

            // Keyhole - dark background color for contrast
            g2d.setColor(new Color(60, 63, 82, 255));
            g2d.fillOval(cx - 5, cy + 8, 10, 10);

            // Keyhole highlight - very subtle
            g2d.setColor(new Color(100, 103, 122, 220));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(cx - 5, cy + 8, 10, 10);

            // Lock shackle highlight for depth
            g2d.setColor(new Color(180, 248, 255, 100));
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawArc(cx - 14, cy - 16, 28, 28, 15, 150);

            g2d.dispose();

            // Create directory if needed
            logoFile.getParentFile().mkdirs();
            ImageIO.write(image, "PNG", logoFile);
            System.out.println("[Logo] Generated successfully: " + outputPath);

        } catch (Exception e) {
            System.err.println("[Logo] Failed to generate: " + e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("[App] Shutting down...");
        
        // Release all .env file locks before exit
        EnvFileLocker.releaseAllLocks();
        
        DatabaseManager.getInstance().close();
    }

    public static void setRoot(Parent root, int width, int height) {
        Scene scene = primaryStage.getScene();
        scene.setRoot(root);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
