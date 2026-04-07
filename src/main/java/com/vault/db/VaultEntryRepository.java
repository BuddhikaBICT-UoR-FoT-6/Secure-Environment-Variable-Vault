package com.vault.db;

import com.vault.model.VaultEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// CRUD operations for encrypted environment variables
// handles saving the iv and ciphertext as Hex Strings
// only stores and retrieves what it is given

public class VaultEntryRepository {
    private final Connection conn;

    public VaultEntryRepository(Connection conn){
        this.conn = conn;

    }

    // saves a new entry or updates an existing one
    // @param entry the VaultEntry holding the encrypted hex strings
    public void saveEntry(VaultEntry entry){
        if(entry.getId() == -1){
            insert(entry);
        } else {
            update(entry);
        }

    }

    private void insert(VaultEntry entry){
        String sql = "INSERT INTO vault_entries (project_id, key_name, iv_hex, ciphertext_hex, is_locked, lock_type, lock_data) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try(PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            stmt.setInt(1, entry.getProjectId());
            stmt.setString(2, entry.getKeyName());
            stmt.setString(3, entry.getIvHex());
            stmt.setString(4, entry.getCiphertextHex());
            stmt.setInt(5, entry.isLocked() ? 1 : 0);
            stmt.setString(6, entry.getLockType());
            stmt.setString(7, entry.getLockData());

            stmt.executeUpdate();

            try(ResultSet keys = stmt.getGeneratedKeys()){
                if(keys.next()){
                    entry.setId(keys.getInt(1));
                }
            }
        } catch(SQLException e){
            throw new RuntimeException("Failed to insert vault entry: " + entry.getKeyName(), e);
        }
    }

    private void update(VaultEntry entry){
        String sql = "UPDATE vault_entries SET key_name = ?, iv_hex = ?, ciphertext_hex = ?, is_locked = ?, lock_type = ?, lock_data = ? WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setString(1, entry.getKeyName());
            stmt.setString(2, entry.getIvHex());
            stmt.setString(3, entry.getCiphertextHex());
            stmt.setInt(4, entry.isLocked() ? 1 : 0);
            stmt.setString(5, entry.getLockType());
            stmt.setString(6, entry.getLockData());
            stmt.setInt(7, entry.getId());

            stmt.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Failed to update vault entry: " + entry.getKeyName(), e);
        }
    }

    // loads ALL encrypted entries for a specific project
    // @param projectId the parent project's database ID
    // @return List of VaultEntries
    public List<VaultEntry> listEntriesForProject(int projectId){
        String sql = "SELECT id, project_id, key_name, iv_hex, ciphertext_hex, is_locked, lock_type, lock_data FROM vault_entries WHERE project_id = ?";

        List<VaultEntry> entries = new ArrayList<>();

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, projectId);

            try(ResultSet rs = stmt.executeQuery()){
                while(rs.next()){
                    VaultEntry entry = new VaultEntry(
                            rs.getInt("id"),
                            projectId,
                            rs.getString("key_name"),
                            rs.getString("iv_hex"),
                            rs.getString("ciphertext_hex"),
                            rs.getInt("is_locked") == 1,
                            rs.getString("lock_type")
                    );
                    entry.setLockData(rs.getString("lock_data"));

                    entries.add(entry);

                }

            }

        } catch(SQLException e){
            throw new RuntimeException("Failed to list vault entries for project id: " + projectId, e);
        }
        return entries;

    }

    // Delete
    public void deleteEntry(int entryId){
        String sql = "DELETE FROM vault_entries WHERE id = ?";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, entryId);

            stmt.executeUpdate();
        } catch(SQLException e){
            throw new RuntimeException("Failed to delete vault entry id: " + entryId, e);
        }

    }

    /**
     * Converts a byte array to a lowercase Hexadecimal string.
     * E.g. [0x0F, 0x1A] -> "0f1a"
     * Used when saving IV and ciphertext to the database.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    /**
     * Converts a Hexadecimal string back to a byte array.
     * Used when reading IV and ciphertext from the database before decryption.
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

}
