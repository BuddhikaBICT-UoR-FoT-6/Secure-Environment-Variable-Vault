package com.vault.model;

import com.vault.crypto.CryptoEngine;
import com.vault.db.VaultEntryRepository;

import javax.crypto.SecretKey;
import java.util.Arrays;

/**
 * UnlockedVault — the in-memory session object for an active session.
 */
public class UnlockedVault {
    private SecretKey secretKey;
    private boolean locked;

    public UnlockedVault(SecretKey secretKey){
        this.secretKey = secretKey;
        this.locked = false;
    }

    public boolean isUnlocked() {
        return !locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public SecretKey getSecretKey() {
        if(locked){
            throw new IllegalStateException("Vault is locked.");
        }
        return secretKey;
    }

    /**
     * Convenience method to encrypt a key-value pair into a VaultEntry for this session.
     */
    public VaultEntry encryptEntry(String keyName, String plaintext) {
        if (locked) throw new IllegalStateException("Vault is locked.");

        CryptoEngine.EncryptedPayload payload = CryptoEngine.encryptString(plaintext, secretKey);
        
        // Create unpersisted VaultEntry
        VaultEntry entry = new VaultEntry(-1, keyName, 
                VaultEntryRepository.bytesToHex(payload.iv()), 
                VaultEntryRepository.bytesToHex(payload.ciphertext()));
        
        return entry;
    }

    public void lock(){
        this.locked = true;
        if(secretKey != null){
            try{
                java.lang.reflect.Field keyField = secretKey.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
                byte[] rawKeyBytes = (byte[]) keyField.get(secretKey);
                if(rawKeyBytes != null){
                    Arrays.fill(rawKeyBytes, (byte) 0x00);
                }
            } catch(Exception e){
                System.err.println(" [Vault] Warning: could not wipe SecretKey: " + e.getMessage());
            }
            secretKey = null;
        }
    }
}
