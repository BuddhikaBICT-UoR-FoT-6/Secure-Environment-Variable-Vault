package com.vault.crypto;
// master password is human-memorable but AES-256 needs a 256-bit cryptographic key, so the password cant be used directly
// master password gets mathematically transformed into an AES-256
// encryption key using PBKDF2 (Password-Based Key Derivation Function 2) with HMAC-SHA256 as the underlying pseudorandom function
// password + salt → [HMAC-SHA256 × 310,000 rounds] → 256-bit key
// The salt is a random byte array stored in the database.
// It ensures two users with the same password get DIFFERENT keys.

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public final class KeyDerivation {
    // PBKDF2 using HMAC-SHA256 as its pseudorandom function
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    // number of iterations for the key derivation function, higher is more secure but slower
    private static final int ITERATIONS = 310_000;

    // output key length in bits for AES-256, which is 256 bits = 32 bytes
    private static final int KEY_LENGTH_BITS = 256;

    // salt length in bytes, 16 bytes = 128 bits of randomness
    public static final int SALT_LENGTH_BYTES = 16;

    private KeyDerivation() {}

    // Generates a brand new random salt
    // call this once when the user sets up their vaut for the first time
    // store the result in the database and it will be needed
    // when every time the user enters their master password to derive the same key
    // @ return a 16-byte cryptographically random salt
    public static byte[] generateSalt(){
        // secureRandom is thread-safe and seeded automatically by the JVM/OS
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LENGTH_BYTES]; // allocate 16 bytes array

        random.nextBytes(salt); // fill every byte with random data

        return salt;
    }

    // Derives a 256-bit AES key from the given password and salt using PBKDF2
    // after calling this method, the caller should wipe the password char[] from memory using SecureMemory.wipe(password)
    // @param password the random salt retrieved from the database
    // @param salt the random salt retrieved from the database
    // @return a SecretKey ready to pass into CryptoEngine encrypt/decrypt
    // @throws NoSuchAlgorithmException if the PBKDF2 algorithm is not available in the JVM
    public static SecretKey deriveKey(char[] password, byte[] salt){
        try{
            // password - what the user typed (char[])
            // salt - random bytes stored in the database
            // iterations - 310,000 rounds of hashing to make it slow and resistant to brute-force attacks
            // keyLength - 256 bits for AES-256
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);

            // performs the actual PBKDF2 computation and tell it which algorithm to use
            // getInstance() looks up the algorithm in the JCE provider registry
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

            // applies HMAC-SHA256 310,000 times to the password and salt
            // then returns the final 256-bit output as a SecretKey
            // the result is a raw key and format is raw bytes
            byte[] rawKey = factory.generateSecret(spec).getEncoded();

            // wipe the PBEKeySpec immediately - it holds a copy of the password
            spec.clearPassword();

            // wraps the raw byte[] into a proper SecretKey object
            // that Java's Cipher class can accept
            // AES tells the JCE to interpret these 256 bits as an AES key
            SecretKey secretKey  = new SecretKeySpec(rawKey, "AES");

            // wipe the raw key bytes since having SecretKeySpec intermediate byte[]
            // is no longer needed in plaintext
            SecureMemory.wipe(rawKey);

            return secretKey;

        } catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            // these exceptions should never happen in a properly configured JVM
            // since PBKDF2WithHmacSHA256 is a standard algorithm that must be supported
            // if it does happen, it indicates a critical misconfiguration of the Java environment
            throw new RuntimeException("PBKDF2 algorithm not available", e);
        }

    }



}
