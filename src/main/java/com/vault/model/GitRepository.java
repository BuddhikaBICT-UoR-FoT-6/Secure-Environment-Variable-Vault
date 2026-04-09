package com.vault.model;

/**
 * GitRepository — represents one row in the "git_repositories" SQLite table.
 */
public class GitRepository {

    private int id;
    private String name;
    private String localPath;
    private String createdAt;

    public GitRepository(int id, String name, String localPath, String createdAt) {
        this.id = id;
        this.name = name;
        this.localPath = localPath;
        this.createdAt = createdAt;
    }

    public GitRepository(String name, String localPath) {
        this.id = -1;
        this.name = name;
        this.localPath = localPath;
        this.createdAt = null;
    }

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
        return "GitRepository { id=" + id + ", name='" + name + "' }";
    }
}
