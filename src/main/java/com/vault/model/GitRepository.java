package com.vault.model;

/**
 * GitRepository — represents one row in the "git_repositories" SQLite table.
 * A GitRepository is a locally cloned GitHub/Git repo that the user has linked
 * to the vault for automatic env key scanning and usage tracking.
 */
public class GitRepository {

    private int id;

    // Human-readable label the user assigns (e.g. "My Node API")
    private String name;

    // Absolute path to the local clone (e.g. C:\Users\HP\Projects\my-api)
    private String localPath;

    // ISO-8601 timestamp from SQLite DEFAULT (datetime('now'))
    private String createdAt;

    // Full constructor — used by GitRepositoryRepository when reading from DB
    public GitRepository(int id, String name, String localPath, String createdAt) {
        this.id = id;
        this.name = name;
        this.localPath = localPath;
        this.createdAt = createdAt;
    }

    // Creation constructor — used when the user links a new repo in the UI
    public GitRepository(String name, String localPath) {
        this.id = -1; // not yet persisted
        this.name = name;
        this.localPath = localPath;
        this.createdAt = null;
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "GitRepository { id=" + id + ", name='" + name + "', path='" + localPath + "' }";
    }
}
