package com.vault.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

/**
 * EnvFileLocker — Manages exclusive locks on .env files
 * to prevent modifications outside this application.
 */
public class EnvFileLocker {
    private static final Map<String, FileLock> activeLocks = new HashMap<>();
    private static final Map<String, RandomAccessFile> openFiles = new HashMap<>();

    /**
     * Attempts to acquire an exclusive lock on a .env file.
     * Returns true if lock was successful.
     */
    public static boolean lockEnvFile(String filePath) {
        try {
            File envFile = new File(filePath);
            if (!envFile.exists()) {
                System.err.println("[EnvLocker] File does not exist: " + filePath);
                return false;
            }

            RandomAccessFile raf = new RandomAccessFile(envFile, "rw");
            FileLock lock = raf.getChannel().tryLock();

            if (lock != null) {
                activeLocks.put(filePath, lock);
                openFiles.put(filePath, raf);
                System.out.println("[EnvLocker] Locked: " + filePath);
                return true;
            } else {
                raf.close();
                System.err.println("[EnvLocker] Could not acquire lock: " + filePath);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[EnvLocker] Failed to lock file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Releases the lock on a .env file.
     */
    public static void unlockEnvFile(String filePath) {
        try {
            FileLock lock = activeLocks.remove(filePath);
            RandomAccessFile raf = openFiles.remove(filePath);

            if (lock != null && lock.isValid()) {
                lock.release();
                System.out.println("[EnvLocker] Unlocked: " + filePath);
            }

            if (raf != null) {
                raf.close();
            }
        } catch (Exception e) {
            System.err.println("[EnvLocker] Failed to unlock file: " + e.getMessage());
        }
    }

    /**
     * Checks if a .env file is currently locked by this application.
     */
    public static boolean isLocked(String filePath) {
        return activeLocks.containsKey(filePath);
    }

    /**
     * Releases all locks when the application exits.
     */
    public static void releaseAllLocks() {
        for (String filePath : new java.util.ArrayList<>(activeLocks.keySet())) {
            unlockEnvFile(filePath);
        }
    }
}
