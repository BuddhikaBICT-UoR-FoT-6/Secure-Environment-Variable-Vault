package com.vault.model;

// VaultEntry — represents one row in the "vault_entries" SQLite table
// A VaultEntry is a single environment variable secret, stored as an encrypted value in the database.


public class VaultEntry {
    // fields
    private int id;

    private int projectId;

    // environment variable name in PLAINTEXT
    private String keyName;

    // the IV and ciphertext are stored as hexadecimal strings in the database
    private String ivHex;

    private String ciphertextHex;

    // This field is populated in-memory ONLY after the user unlocks the vault
    private transient String decryptedValue;

    // Whether this key requires biometric (face) authentication to view
    private boolean isLocked;

    // Type of lock: "none" or "face"
    private String lockType;

    // Stores the path to the face crop OR the hash of the PIN
    private String lockData;

    // Full constructor — used by VaultEntryRepository when reading rows from the DB
    public VaultEntry(int id, int projectId, String keyName, String ivHex, String ciphertextHex, boolean isLocked, String lockType){
        this.id = id;
        this.projectId = projectId;
        this.keyName = keyName;
        this.ivHex = ivHex;
        this.ciphertextHex = ciphertextHex;
        this.isLocked = isLocked;
        this.lockType = lockType != null ? lockType : "none";
    }

    // Legacy constructor for backward compatibility
    public VaultEntry(int id, int projectId, String keyName, String ivHex, String ciphertextHex){
        this(id, projectId, keyName, ivHex, ciphertextHex, false, "none");
    }

    // Creation constructor — used when the user creates a new vault entry in the UI
    public VaultEntry(int projectId, String keyName, String ivHex, String ciphertextHex){
        this.id = -1; // not yet persisted to database
        this.projectId = projectId;
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

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
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

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getLockType() {
        return lockType;
    }

    public void setLockType(String lockType) {
        this.lockType = lockType != null ? lockType : "none";
    }

    public String getLockData() {
        return lockData;
    }

    public void setLockData(String lockData) {
        this.lockData = lockData;
    }

    @Override
    public String toString(){
        return "ValutEntry {id = " + id + " , projectId = " + projectId + " , key = ," + keyName + "'}";
    }


}
