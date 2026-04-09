package com.vault.db;

import com.vault.model.GitRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GitRepositoryRepository — handles all CRUD operations for the git_repositories table.
 */
public class GitRepositoryRepository {

    private final Connection connection;

    public GitRepositoryRepository() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Inserts a new repository record and sets the generated ID on the model.
     *
     * @param repo the GitRepository to persist (id will be set after insert)
     */
    public void save(GitRepository repo) {
        String sql = "INSERT INTO git_repositories (name, path, project_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, repo.getName());
            ps.setString(2, repo.getLocalPath());
            ps.setInt(3, repo.getProjectId());
            ps.executeUpdate();

            // Set the DB-generated ID back on the model
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    repo.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save GitRepository: " + repo.getName(), e);
        }
    }

    /**
     * Returns all linked repositories.
     */
    public List<GitRepository> findAll() {
        List<GitRepository> repos = new ArrayList<>();
        String sql = "SELECT id, project_id, name, path, created_at FROM git_repositories ORDER BY created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                repos.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch git repositories", e);
        }
        return repos;
    }

    /**
     * Finds a single repository by its primary key.
     */
    public Optional<GitRepository> findById(int id) {
        String sql = "SELECT id, project_id, name, path, created_at FROM git_repositories WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch repository with id: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Deletes a repository by ID.
     */
    public void delete(int id) {
        String sql = "DELETE FROM git_repositories WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete repository with id: " + id, e);
        }
    }

    // Maps a ResultSet row to a GitRepository model
    private GitRepository mapRow(ResultSet rs) throws SQLException {
        return new GitRepository(
                rs.getInt("id"),
                rs.getInt("project_id"),
                rs.getString("name"),
                rs.getString("path"),
                rs.getString("created_at")
        );
    }
}
