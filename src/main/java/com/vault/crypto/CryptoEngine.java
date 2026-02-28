package com.vault.crypto;
// AES-256 - encryption algorithm unbreakable by brute force
// GCM (Galois/Counter Mode) - gives authenticated encryption where it detects
// if anyone tampered with the ciphertext, if so returns exception

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey; // configure with an algorithm
import javax.crypto.spec.GCMParameterSpec; // tells the cipher how long the auth tag is
import java.security.SecureRandom;

public final class CryptoEngine {
    // full algorithm string passed. NoPadding = GCM works on any length; no block-padding needed
    // Algorithm/Mode/Padding
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // IV - initialization vector, random bytes that make the same plaintext encrypt to different ciphertexts each time
    // reusing an IV with the same key can completely break GCM security
    private static final int IV_LENGTH_BYTES = 12;

    // authentication tag length in bits, appended by Java to the end of the ciphertext automatically
    private static final int TAG_LENGTH_BITS = 128;

    // private constructor to prevent instantiation
    private CryptoEngine() {}

    // a simple container that bundles together everything needed to later decrypt a value
    public record EncryptedPayload(byte[] iv, byte[] ciphertext) {}

    // encrypts the plaintext using AES-256-GCM with the provided key, returns the IV and ciphertext together
    // @param plaintext the data to encrypt, as bytes
    // @param key the AES-256 SecretKey derived from the master password
    // @return an EncryptedPayload containing the random IV and the ciphertext
    public static EncryptedPayload encrypt(byte[] plaintext, SecretKey key){
        try{
            // generate a unique random IV
            // every encrypt generates a new random IV,
            // so the same plaintext will encrypt to different ciphertexts each time, preventing pattern recognition attacks
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH_BYTES]; // allocate 12 bytes array
            random.nextBytes(iv); // fill with random data

            // configure the cipher
            // looks up "AES/GCM/NoPadding" in the JCE registry
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // GCMParameterSpec packages
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            // initialize the cipher for encryption mode with the key (AES-256) and IV and tag length config
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // perform the encryption, returns the ciphertext with the auth tag appended at the end
            byte[] ciphertext = cipher.doFinal(plaintext);

            // bundle the IV and ciphertext together in a simple record class and return it
            // both must be stored in the database and without the IV, decryption will fail
            return new EncryptedPayload(iv, ciphertext);

        } catch(Exception e){
            throw new RuntimeException("Encryption failed", e);
        }

    }

    // decrypts an EncryptedPayload back to the original plaintext bytes
    // @param payload the EncryptedPayload containing the IV and ciphertext to decrypt
    // @param key the AES-256 SecretKey derived from the master password
    // @return the original plaintext bytes if decryption is successful
    // @throws AEADBadTagException if the ciphertext was tampered with or the wrong key was used
    public static byte[] decrypt(EncryptedPayload payload, SecretKey key){
        try{
            // Reconstruct the cipher in decryption mode with the same key and IV
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // use exact same IV that was used when encrypted
            GCMParameterSpec parameterSpec  = new GCMParameterSpec(TAG_LENGTH_BITS, payload.iv);

            // DECRYPT_MODE -> cipher runs in reverse
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // perform the decryption, returns the original plaintext if successful
            // Verifies the 128-bit authentication tag at the end of the ciphertext
            // If the tag doesn't match (tampering or wrong key), throws AEADBadTagException
            return cipher.doFinal(payload.ciphertext);

        } catch(Exception e){
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // helper method that encrypts a string value directly
    // internally converts string to utf-8 bytes, then encrypts
    // @param plaintext the string to encrypt
    // @param key the AES-256 SecretKey derived from the master password
    // @return an EncryptedPayload containing the IV and ciphertext ready to store in the database
    public static EncryptedPayload encryptString(String value, SecretKey key){
        // convert each character in the string to its UTF-8 byte representation
        return encrypt(value.getBytes(java.nio.charset.StandardCharsets.UTF_8), key);
    }

    // helper method that decrypts an EncryptedPayload back to a string
    // internally decrypts to bytes, then converts back to a UTF-8 string
    // @param payload the EncryptedPayload containing the IV and ciphertext to decrypt
    // @param key the AES-256 SecretKey derived from the master password
    // @return the original plaintext string if decryption is successful
    public static String decryptString(EncryptedPayload payload, SecretKey key){
        byte[] decryptedBytes = decrypt(payload, key);

        //convert the decrypted UTF-8 bytes back to a string
        String result = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);

        // wipe the decrypted bytes from memory immediately after converting to string
        SecureMemory.wipe(decryptedBytes);

        return result;
    }
}
