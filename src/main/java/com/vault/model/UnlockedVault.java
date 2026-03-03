package com.vault.model;

import javax.crypto.SecretKey;
import java.util.Arrays;

// the in-memory session object for an active, authenticated session
// one instance of this exists while the vault is open, it has
// - the live AES-256 SecretKey, a locked/unlocked state flag
// after lock(), this object is useless — a new one must be created
// by deriving the key again from the master password

public class UnlockedVault {
    // live AES-256 SecretKey derived from the master password + salt
    // all encrypt/decrypt operations use this key and Set to null after lock() is called
    private SecretKey secretKey;

    // flag to indicate if the vault is currently unlocked
    // when true, secretKey is null and no decryption is possible
    // when false, secretKey is valid and decryption can be performed
    // this flag is used to prevent accidental use of the secretKey after lock() is called
    // volatile ensures the UI thread sees the lock immediately
    private boolean locked;

    // creates a new unlocked session
    // @param secretKey the AES-256 key derived from the correct master password
    public UnlockedVault(SecretKey secretKey){
        this.secretKey = secretKey;
        this.locked = false; // starts unlocked — the user just authenticated
    }

    // returns true if the vault is currently unlocked (usable)
    public boolean isUnlocked() {
        return !locked;
    }

    // returns true if the vault is currently locked
    public boolean isLocked() {
        return locked;
    }

    // returns the live SecretKey for use in CryptoEngine operations
    // throws IllegalStateException if the vault is locked (key is null)
    public SecretKey getSecretKey() {
        if(locked){
            throw new IllegalStateException("Vault is locked. Cannot retrieve SecretKey. Call isUnlocked() before getSecretKey().");
        }
        return secretKey;
    }

    // locks the vault by clearing the SecretKey and setting the locked flag
    // after this is called, the vault is unusable until a new UnlockedVault is created with a valid SecretKey
    public void lock(){
        this.locked = true; // set locked flag first to prevent any further use of the key

        if(secretKey != null){
            // attempt zero-wipe via reflection
            try{
                // javax.crypto.spec.SecretKeySpec stores the key in a private byte[] field named "key"
                java.lang.reflect.Field keyField =
                        secretKey.getClass().getDeclaredField("key");

                // bypasses the private access modifier to reach private internals
                keyField.setAccessible(true);

                // get the byte[] that holds the key material
                byte[] rawKeyBytes = (byte[]) keyField.get(secretKey);

                // overwrite the key bytes with zeros to attempt to clear it from memory
                if(rawKeyBytes != null){
                    Arrays.fill(rawKeyBytes, (byte) 0x00);
                }

                System.out.println(" [Vault] SecretKey bytes zeroed out in memory.");

            } catch(NoSuchFieldException | IllegalAccessException e){
                System.err.println(" [Vault] Warning: could not wipe SecretKey bytes via reflection: " + e.getMessage());

            }

            // finally, set the reference to null to allow GC to reclaim it
            secretKey = null;
        }
        System.out.println(" [Vault] Vault locked. SecretKey cleared from memory.");
    }
}
