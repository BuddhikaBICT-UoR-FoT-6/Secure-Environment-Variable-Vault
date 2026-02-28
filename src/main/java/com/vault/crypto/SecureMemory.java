package com.vault.crypto;
// SecureMemory is a utility class that provides methods to securely wipe sensitive data from memory.
// after finish using a password or decrypted key
// call SecureMemory.wipe(char[] data) to overwrite the contents with 0x00 bytes so the secret
// cannot be recovered from a heap dump or memory scan
public final class SecureMemory {
    // private constructor to prevent instantiation
    // this is a utility class with static methods only, no instances needed
    private SecureMemory() {}

    // wipes a char array by overwriting every position with null char
    // use this for: master password char[] after key derivation
    // @param data the sensitive char array to destroy
    public static void wipe(char[] data){
        if(data == null) return;

        // iterate over every index in the array
        for(int i = 0; i < data.length; i++){
            data[i] = '\0'; // '\0' = the null character, ASCII value 0
            // replaces whatever secret char was there

        }
    }

    // wipes a byte array by overwriting every position with 0x00
    // use this for: derived secretkey bytes, decrypted plaintext bytes
    // which are bytes after encryption or decryption operations
    // @param data the sensitive byte array to destroy
    public static void wipe(byte[] data){
        if(data == null) return;

        for(int i = 0; i < data.length; i++){
            data[i] = 0x00; // overwrite with zero byte - 0x00 = hexadecimal for 0, same as (byte)0
        }

    }

}