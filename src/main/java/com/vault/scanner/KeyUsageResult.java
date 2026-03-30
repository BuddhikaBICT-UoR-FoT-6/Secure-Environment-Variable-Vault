package com.vault.scanner;

/**
 * KeyUsageResult — a lightweight value object representing one occurrence
 * of an environment variable key found inside a source file.
 *
 * Used by KeyUsageTracker to return scan results to the UI layer.
 */
public class KeyUsageResult {

    // File path relative to the repository root (e.g. "src/config/db.js")
    private final String relativeFilePath;

    // 1-based line number where the key was found
    private final int lineNumber;

    // The raw content of that line (trimmed for display)
    private final String lineContent;

    public KeyUsageResult(String relativeFilePath, int lineNumber, String lineContent) {
        this.relativeFilePath = relativeFilePath;
        this.lineNumber = lineNumber;
        this.lineContent = lineContent;
    }

    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }

    @Override
    public String toString() {
        return relativeFilePath + ":" + lineNumber + " → " + lineContent;
    }
}
