package com.vault.scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * EnvKeyScanner — walks a local git repository directory and discovers all
 * environment variable keys defined in .env files.
 *
 * Scans: .env, .env.local, .env.example, .env.production, .env.development, etc.
 * Parses KEY=VALUE lines (ignores comments # and blank lines).
 */
public class EnvKeyScanner {

    // Patterns that match common .env file names
    private static final List<String> ENV_FILE_PATTERNS = List.of(
            ".env", ".env.local", ".env.example", ".env.development",
            ".env.production", ".env.staging", ".env.test"
    );

    private final String repoRootPath;

    public EnvKeyScanner(String repoRootPath) {
        this.repoRootPath = repoRootPath;
    }

    /**
     * Scans all .env files in the repository and returns discovered keys.
     *
     * @return Map of key name -> source file path (relative to repo root)
     */
    public Map<String, String> scan() {
        Map<String, String> discoveredKeys = new LinkedHashMap<>();
        Path root = Paths.get(repoRootPath);

        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(this::isEnvFile)
                .forEach(envFile -> parseEnvFile(envFile, root, discoveredKeys));
        } catch (IOException e) {
            System.err.println("[EnvKeyScanner] Error walking repo: " + e.getMessage());
        }

        System.out.println("[EnvKeyScanner] Found " + discoveredKeys.size() + " keys in " + repoRootPath);
        return discoveredKeys;
    }

    /**
     * Returns a flat list of unique key names only (without source paths).
     */
    public List<String> scanKeyNames() {
        return new ArrayList<>(scan().keySet());
    }

    /**
     * Returns all .env file paths found in the repo root (not recursive for security).
     */
    public List<Path> findEnvFiles() {
        List<Path> found = new ArrayList<>();
        Path root = Paths.get(repoRootPath);
        try (Stream<Path> walk = Files.walk(root, 3)) { // limit depth to 3
            walk.filter(Files::isRegularFile)
                .filter(this::isEnvFile)
                .forEach(found::add);
        } catch (IOException e) {
            System.err.println("[EnvKeyScanner] Error finding env files: " + e.getMessage());
        }
        return found;
    }

    // Returns true if the given path looks like a .env file
    private boolean isEnvFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return ENV_FILE_PATTERNS.contains(fileName) || fileName.startsWith(".env.");
    }

    // Parses a single .env file and adds discovered keys to the result map
    private void parseEnvFile(Path envFile, Path root, Map<String, String> result) {
        String relativePath = root.relativize(envFile).toString();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip blank lines and comments
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Parse KEY=VALUE (or KEY= with empty value)
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    // Key must be valid identifier: letters, digits, underscores only
                    if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                        result.put(key, relativePath);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[EnvKeyScanner] Failed to read: " + envFile + " — " + e.getMessage());
        }
    }
}
