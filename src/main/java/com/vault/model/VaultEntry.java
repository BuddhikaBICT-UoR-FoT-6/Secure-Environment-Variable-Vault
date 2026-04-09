package com.vault.model;

// VaultEntry — represents one row in the "vault_entries" SQLite table
// A VaultEntry is a single environment variable secret, stored as an encrypted value in the database.


public class VaultEntry {
    // fields
    private int id;

    private int repositoryId;

    // environment variable name in PLAINTEXT
    private String keyName;

    // the IV and ciphertext are stored as hexadecimal strings in the database
    private String ivHex;

    private String ciphertextHex;

    // This field is populated in-memory ONLY after the user unlocks the vault
    private transient String decryptedValue;

    // Full constructor — used by VaultEntryRepository when reading rows from the DB
    public VaultEntry(int id, int repositoryId, String keyName, String ivHex, String ciphertextHex){
        this.id = id;
        this.repositoryId = repositoryId;
        this.keyName = keyName;
        this.ivHex = ivHex;
        this.ciphertextHex = ciphertextHex;
    }

    // Creation constructor — used when the user creates a new vault entry in the UI
    public VaultEntry(int repositoryId, String keyName, String ivHex, String ciphertextHex){
        this.id = -1; // not yet persisted to database
        this.repositoryId = repositoryId;
        this.keyName = keyName;
        this.ivHex = ivHex;
        this.ciphertextHex = ciphertextHex;
    }

    // getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(int repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getIvHex() {
        return ivHex;
    }

    public void setIvHex(String ivHex) {
        this.ivHex = ivHex;
    }

    public String getCiphertextHex() {
        return ciphertextHex;
    }

    public void setCiphertextHex(String ciphertextHex) {
        this.ciphertextHex = ciphertextHex;
    }

    // Returns the in-memory decrypted value
    // will be null if the vault is locked or this entry hasn't been decrypted yet
    public String getDecryptedValue() {
        return decryptedValue;
    }

    public void setDecryptedValue(String decryptedValue) {
        this.decryptedValue = decryptedValue;
    }

    // clears the decrypted value from memory
    public void clearDecryptedValue(){
        this.decryptedValue = null;
    }

    @Override
    public String toString(){
        return "VaultEntry {id = " + id + " , repositoryId = " + repositoryId + " , key = ," + keyName + "'}";
    }


}
