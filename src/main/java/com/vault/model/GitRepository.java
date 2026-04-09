package com.vault.model;

/**
 * GitRepository — represents one row in the "git_repositories" SQLite table.
 */
public class GitRepository {

    private int id;
    private int projectId;
    private String name;
    private String localPath;
    private String createdAt;

    public GitRepository(int id, int projectId, String name, String localPath, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.localPath = localPath;
        this.createdAt = createdAt;
    }

    public GitRepository(String name, String localPath) {
        this.id = -1;
        this.projectId = -1;
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

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
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
        return "GitRepository { id=" + id + ", projectId=" + projectId + ", name='" + name + "' }";
    }
}
