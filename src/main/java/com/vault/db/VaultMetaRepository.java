package com.vault.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

import com.vault.crypto.KeyDerivation;

/**
 * Handles database operations for the vault_meta table.
 * Principally used for storing and retrieving the global PBKDF2 salt.
 */
public class VaultMetaRepository {
    private final Connection conn;

    public VaultMetaRepository(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves the PBKDF2 salt for the master password from the database.
     * 
     * @return The salt as a Hex String, or null if it doesn't exist yet (first
     *         launch).
     */
    public String getSaltHex() {
        String sql = "SELECT salt_hex FROM vault_meta WHERE id = 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("salt_hex");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve salt_hex from vault_meta", e);
        }
        return null;
    }

    /**
     * Saves the PBKDF2 salt for the master password.
     * 
     * @param saltHex The 16-byte salt, converted to a Hex String.
     */
    public void saveSaltHex(String saltHex) {
        // SQLite supports INSERT OR REPLACE to handle upserts easily
        String sql = "INSERT OR REPLACE INTO vault_meta (id, salt_hex) VALUES (1, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, saltHex);
            stmt.executeUpdate();

            System.out.println("[VaultMetaRepo] Saved master salt to database.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save salt_hex to vault_meta", e);
        }
    }

    /**
     * Retrieves the existing salt if present, otherwise generates a new 16-byte
     * secure salt,
     * saves it to the database, and returns it.
     * 
     * @return The master salt as a Hex String.
     */
    public String getOrGenerateSalt() {
        String existingSalt = getSaltHex();
        if (existingSalt != null) {
            return existingSalt;
        }

        // Generate a new 16-byte secure salt
        byte[] saltBytes = KeyDerivation.generateSalt();

        // Convert explicitly to Hex String
        String newSaltHex = HexFormat.of().formatHex(saltBytes);

        // Save the newly generated salt in the DB
        saveSaltHex(newSaltHex);

        return newSaltHex;
    }

    /**
     * Clears all vault data from the database.
     * This deletes all projects, repositories, and vault entries.
     * WARNING: This action cannot be undone!
     */
    public void clearAllVaultData() {
        try {
            // Delete all vault entries
            String deleteEntries = "DELETE FROM vault_entries";
            try (PreparedStatement stmt = conn.prepareStatement(deleteEntries)) {
                stmt.executeUpdate();
            }

            // Delete all repositories
            String deleteRepos = "DELETE FROM git_repositories";
            try (PreparedStatement stmt = conn.prepareStatement(deleteRepos)) {
                stmt.executeUpdate();
            }

            // Delete all projects
            String deleteProjects = "DELETE FROM projects";
            try (PreparedStatement stmt = conn.prepareStatement(deleteProjects)) {
                stmt.executeUpdate();
            }

            System.out.println("[VaultMetaRepo] All vault data cleared successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear vault data: " + e.getMessage(), e);
        }
    }
}
