package com.vault.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// DatabaseManager is responsible for managing the SQLite database connection and schema
// Responsibilities:
// - determine where to store the value.db file
// - open a JDBC connection to SQLite
// - create all required tables if they dont exist
// - provide that connection to repository classes

public final class DatabaseManager {
    // for thread-safe lazy initialisation, volatile make sure
    // every thread must read it fresh from main memory
    private static volatile DatabaseManager instance;

    // live JDBC connection to the SQLite database, shared across the app
    private Connection connection;

    // path to the directory where vault.db is stored
    // System.getProperty("user.home") = C:\Users\YourName
    // append "/.envvault" to keep app data isolated from the project
    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".envvault";

    // final db path -> C:\Users\YourName\.envvault\vault.db
    private static final String DB_PATH = DB_DIR + File.separator + "vault.db";

    // When DriverManager.getConnection() sees this URL, it knows to use
    //     * the SQLite JDBC driver (from our pom.xml dependency) to open the file
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    // all the setup work happens here
    private DatabaseManager(){
        createDirectoryIfNeeded(); // step 01: ensure ~/.envvault/ folder exists
        openConnection(); // step 02: open a live JDBC connection to the vault.db file
        createSchema(); // step 03: create the tables if they dont exist
    }

    // singleton pattern with double-checked locking for thread-safe lazy initialization
    public static DatabaseManager getInstance(){
        if(instance == null){
            // no locking overhead if instance is already initialized
            // only one thread can enter this block at a time, preventing multiple initializations
            synchronized(DatabaseManager.class){
                if(instance == null){
                    // prevents two threads that both passed the first check from creating an instance
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    // returns the active JDBC connection to the vault.db database, used by repository classes to execute SQL queries
    public Connection getConnection(){
        return connection;
    }

    // close the database connection when the app is shutting down, to free resources and avoid leaks
    public void close(){
        try{
            if(connection != null && !connection.isClosed()){
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e){
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }

    // Creates the ~/.envvault/ directory if it doesn't already exist
    private void createDirectoryIfNeeded(){
        File dir = new File(DB_DIR);

        // returns true if created, false if it already existed
        // don't throw an error if it exists — that's expected after first run
        if(!dir.exists()){
            boolean created = dir.mkdirs();

            if(created){
                System.out.println("Created directory: " + DB_DIR);
            }
        }
    }

    // Opens a JDBC connection to the SQLite database file at DB_PATH
    private void openConnection(){
        try{
            // Explicitly load the driver class for shaded JAR compatibility
            Class.forName("org.sqlite.JDBC");
            
            connection = DriverManager.getConnection(JDBC_URL);
            System.out.println("Database connection established to " + DB_PATH);

            // WAL (Write-Ahead Logging) mode for better concurrency
            // WAL allows reads and writes to happen simultaneously without locking the entire database
            try(Statement st = connection.createStatement()){
                st.execute("PRAGMA journal_mode=WAL;");
            }

        }catch(SQLException | ClassNotFoundException e){
            // If can't open the database, the entire app is useless
            // Wrap and re-throw as an unchecked exception to crash early with a clear message.
            throw new RuntimeException("Failed to open database at: " + DB_PATH, e);
        }

    }

    // Creates the three database tables on first run
    // vault_meta    — stores app-wide settings (the PBKDF2 salt)
    // projects      — one row per project/workspace the user creates
    // vault_entries — the actual Key=Value secrets, encrypted
    private void createSchema(){
        // after the block exits java automatically closes the Statement resource, preventing leaks
        try(Statement st = connection.createStatement()){
            // Table 1: vault_meta
            // Stores global metadata. Row with id=1 always holds the PBKDF2 salt.
            // The salt_hex column stores the 16-byte salt as a hexadecimal String
            st.execute("""
                CREATE TABLE IF NOT EXISTS vault_meta (
                    id       INTEGER PRIMARY KEY,
                    salt_hex TEXT NOT NULL
                );
            """);

            // Table 2: projects
            // One row per project
            // launch_cmd  = the command to run, e.g. "npm run dev"
            st.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    name        TEXT    NOT NULL,
                    launch_cmd  TEXT,
                    working_dir TEXT,
                    created_at  TEXT    DEFAULT (datetime('now'))
                );
            """);

            // Table 3: vault_entries
            // One row per Key=Value pair stored under a project
            // key_name     = the environment variable name, e.g. "DATABASE_URL"
            // stored in PLAINTEXT — variable names are not secret
            //   iv_hex       = the 12-byte AES-GCM IV, stored as hex string
            //   ciphertex
            //   If a project is deleted, all its vault entries are automatically
            st.execute("""
                CREATE TABLE IF NOT EXISTS vault_entries (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id     INTEGER NOT NULL,
                    key_name       TEXT    NOT NULL,
                    iv_hex         TEXT    NOT NULL,
                    ciphertext_hex TEXT    NOT NULL,
                    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );
            """);

            // Enable foreign key constraint enforcement in SQLite.
            st.execute("PRAGMA foreign_keys = ON;");
            System.out.println("Database Schema ready.");

        } catch (SQLException e){
            throw new RuntimeException("Failed to create database schema", e);
        }

    }




}
