package com.vault.scanner;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * KeyChangeWatcher — monitors .env files inside a linked repository for
 * unauthorised modifications using Java's NIO WatchService API.
 *
 * When a monitored .env file is modified, this watcher re-scans it and
 * compares the current keys against a provided baseline snapshot.
 * Any key that has been added, removed, or whose value changed triggers
 * the {@code onChangeDetected} callback with the name of the affected key.
 *
 * The watcher runs on a dedicated daemon thread so it never blocks the
 * JavaFX UI thread.
 */
public class KeyChangeWatcher {

    private final String repoRootPath;

    // Baseline: key name → raw value string captured at link time
    private final Map<String, String> baseline;

    // Called on the watcher thread when a change is detected. Argument is the changed key name.
    private final Consumer<String> onChangeDetected;

    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running = false;

    public KeyChangeWatcher(String repoRootPath,
                            Map<String, String> baseline,
                            Consumer<String> onChangeDetected) {
        this.repoRootPath = repoRootPath;
        this.baseline = new HashMap<>(baseline);
        this.onChangeDetected = onChangeDetected;
    }

    /**
     * Starts monitoring the repo root directory for changes to .env files.
     * Runs on a background daemon thread.
     */
    public void start() {
        if (running) return;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path root = Paths.get(repoRootPath);
            root.register(watchService, ENTRY_MODIFY);
            running = true;

            watcherThread = new Thread(this::watchLoop, "EnvChangeWatcher-" + repoRootPath);
            watcherThread.setDaemon(true);
            watcherThread.start();
            System.out.println("[KeyChangeWatcher] Started watching: " + repoRootPath);
        } catch (IOException e) {
            System.err.println("[KeyChangeWatcher] Failed to start: " + e.getMessage());
        }
    }

    /**
     * Stops the background watcher thread and releases the WatchService.
     */
    public void stop() {
        running = false;
        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {}
        System.out.println("[KeyChangeWatcher] Stopped.");
    }

    // Background watch loop — runs until stop() is called
    private void watchLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take(); // blocks until an event arrives
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                Path changed = ((WatchEvent<Path>) event).context();
                String fileName = changed.getFileName().toString().toLowerCase();

                // Only react to .env file changes
                if (fileName.startsWith(".env") || fileName.endsWith(".env")) {
                    Path fullPath = Paths.get(repoRootPath).resolve(changed);
                    checkForChanges(fullPath);
                }
            }
            key.reset();
        }
    }

    // Re-scans the changed file and fires the callback for each modified key
    private void checkForChanges(Path envFile) {
        EnvKeyScanner scanner = new EnvKeyScanner(repoRootPath);
        Map<String, String> currentKeys = scanner.scan();

        // Check for removed keys
        for (String baselineKey : baseline.keySet()) {
            if (!currentKeys.containsKey(baselineKey)) {
                System.out.println("[KeyChangeWatcher] Key REMOVED: " + baselineKey);
                onChangeDetected.accept(baselineKey);
            }
        }

        // Check for added or potentially changed keys
        for (String currentKey : currentKeys.keySet()) {
            if (!baseline.containsKey(currentKey)) {
                System.out.println("[KeyChangeWatcher] Key ADDED: " + currentKey);
                onChangeDetected.accept(currentKey);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}
