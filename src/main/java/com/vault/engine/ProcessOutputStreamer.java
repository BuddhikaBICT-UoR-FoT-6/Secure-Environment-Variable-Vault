package com.vault.engine;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ProcessOutputStreamer — Bridges the CLI world to the GUI world.
 *
 * Reads output from the running development server (e.g. Node, Maven)
 * and appends it to a JavaFX TextArea in real-time.
 */
public class ProcessOutputStreamer implements Runnable {

    // The stream to read from (pb.start().getInputStream())
    private final InputStream inputStream;

    // The JavaFX UI component where the text will be displayed
    private final TextArea consoleTextArea;

    // Optional callback to notify the UI when the process dies
    private final Runnable onExitCallback;

    public ProcessOutputStreamer(InputStream inputStream, TextArea consoleTextArea, Runnable onExitCallback) {
        this.inputStream = inputStream;
        this.consoleTextArea = consoleTextArea;
        this.onExitCallback = onExitCallback;
    }

    /**
     * This method runs in its own background thread.
     * We MUST NOT run this on the JavaFX thread, otherwise the entire UI
     * will freeze while waiting for terminal output.
     */
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            // readLine() blocks until the process prints a line or terminates
            while ((line = reader.readLine()) != null) {
                // We captured a line of terminal output!
                // Because JavaFX requires all UI updates to happen on the main UI thread,
                // we wrap the append call inside Platform.runLater().
                final String capturedLine = line;
                Platform.runLater(() -> {
                    consoleTextArea.appendText(capturedLine + "\n");
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                consoleTextArea.appendText("\n[Vault] Stream error: " + e.getMessage() + "\n");
            });
        } finally {
            // Once the while loop ends, the child process has died/stopped
            Platform.runLater(() -> {
                consoleTextArea.appendText("\n[Vault] Process exited.\n");
                if (onExitCallback != null) {
                    onExitCallback.run();
                }
            });
        }
    }

    /**
     * Convenience method to spin up the background thread.
     * Setting it as a "daemon" thread ensures that if the user closes
     * the vault window, this thread won't keep the JVM alive.
     */
    public static void startStreaming(InputStream is, TextArea textArea, Runnable onExit) {
        Thread streamerThread = new Thread(new ProcessOutputStreamer(is, textArea, onExit));
        streamerThread.setDaemon(true); // Don't block JVM shutdown
        streamerThread.setName("ConsoleStreamer-Thread");
        streamerThread.start();
    }
}
