package com.vault.util;

import java.io.File;
import java.nio.file.*;

/**
 * EnvFileWatcher — Monitors .env files for access attempts
 * and automatically launches this application when accessed.
 */
public class EnvFileWatcher implements Runnable {
    private final String envFilePath;
    private final Runnable onFileAccessed;
    private volatile boolean running = true;

    public EnvFileWatcher(String envFilePath, Runnable onFileAccessed) {
        this.envFilePath = envFilePath;
        this.onFileAccessed = onFileAccessed;
    }

    @Override
    public void run() {
        try {
            File envFile = new File(envFilePath);
            File parentDir = envFile.getParentFile();

            Path watchPath = parentDir.toPath();
            String fileName = envFile.getName();

            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                watchPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                System.out.println("[EnvWatcher] Watching: " + envFilePath);

                while (running) {
                    WatchKey key = watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (key == null) continue;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path eventPath = (Path) event.context();

                        if (eventPath.toString().equals(fileName)) {
                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                System.out.println("[EnvWatcher] .env file modified: " + fileName);
                                if (onFileAccessed != null) {
                                    onFileAccessed.run();
                                }
                            }
                        }
                    }

                    if (!key.reset()) {
                        System.err.println("[EnvWatcher] Watch key no longer valid");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[EnvWatcher] Error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
