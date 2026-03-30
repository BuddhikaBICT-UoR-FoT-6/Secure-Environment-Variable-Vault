package com.vault.scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * KeyUsageTracker — scans all source files in a repository to find every line
 * that references a given environment variable key name.
 *
 * Searches for common usage patterns:
 *   - process.env.KEY_NAME         (Node.js)
 *   - os.environ.get('KEY_NAME')   (Python)
 *   - System.getenv("KEY_NAME")    (Java)
 *   - getenv("KEY_NAME")           (PHP / C)
 *   - ENV['KEY_NAME']              (Ruby)
 *   - $env:KEY_NAME / $KEY_NAME    (Shell)
 *   - Plain KEY_NAME occurrence    (any file, as fallback)
 *
 * Source files scanned: .js, .ts, .py, .java, .php, .rb, .go, .env*, .yml, .yaml, .sh
 */
public class KeyUsageTracker {

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".js", ".ts", ".jsx", ".tsx", ".py", ".java", ".php",
            ".rb", ".go", ".sh", ".yaml", ".yml", ".env", ".conf", ".properties"
    );

    // Directories to skip — build artefacts, dependencies etc.
    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "target", "build", "dist", "__pycache__", ".idea", ".vscode"
    );

    private final String repoRootPath;

    public KeyUsageTracker(String repoRootPath) {
        this.repoRootPath = repoRootPath;
    }

    /**
     * Searches the repo for all occurrences of {@code keyName}.
     *
     * @param keyName the environment variable name to search for (e.g. "DATABASE_URL")
     * @return list of KeyUsageResult, one per matching line
     */
    public List<KeyUsageResult> findUsages(String keyName) {
        List<KeyUsageResult> results = new ArrayList<>();
        Path root = Paths.get(repoRootPath);

        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> !isInSkippedDir(p, root))
                .filter(this::isSourceFile)
                .forEach(file -> scanFile(file, root, keyName, results));
        } catch (IOException e) {
            System.err.println("[KeyUsageTracker] Error walking repo: " + e.getMessage());
        }

        System.out.println("[KeyUsageTracker] Found " + results.size()
                + " usages of '" + keyName + "' in " + repoRootPath);
        return results;
    }

    // Checks whether any parent in the path is a skipped directory
    private boolean isInSkippedDir(Path file, Path root) {
        Path rel = root.relativize(file);
        for (Path part : rel) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }

    // Returns true if this file's extension is one we want to scan
    private boolean isSourceFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        // Always include .env files
        if (name.startsWith(".env")) return true;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) return false;
        return SOURCE_EXTENSIONS.contains(name.substring(dotIndex));
    }

    // Scans one file line by line and adds matching lines to results
    private void scanFile(Path file, Path root, String keyName, List<KeyUsageResult> results) {
        String relativePath = root.relativize(file).toString().replace("\\", "/");
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.contains(keyName)) {
                    results.add(new KeyUsageResult(relativePath, lineNum, line.trim()));
                }
            }
        } catch (IOException e) {
            // Skip unreadable files silently (binary files, permission issues, etc.)
        }
    }
}
