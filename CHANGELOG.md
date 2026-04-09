# Changelog
All notable changes to the Secure Environment Variable Vault project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.0.0] - 2026-04-10 — **Final Release**

> This is the stable production release. The application manages environment variables
> from `.env` files in linked Git repositories with real-time file monitoring and secure backups.

### Core Features
- **Repository Management**: Link local Git repositories to manage their environment variables
- **Direct .env Management**: Read, update, and delete environment variables directly from `.env` files
- **File Locking**: Prevent external modifications to `.env` files during active sessions
- **File Monitoring**: Real-time detection of manual modifications using NIO WatchService
- **Secure Backups**: Create encrypted backups of `.env` files with PBKDF2-derived encryption (AES-256-GCM)
- **Recursive Search**: Automatically locate `.env` files anywhere in repository hierarchy (excludes .git, node_modules, target)

### Architecture
- **Cryptography**: PBKDF2 key derivation (310,000 iterations) + AES-256-GCM symmetric encryption
- **Database**: SQLite with minimalist schema (only stores metadata and PBKDF2 salt)
- **UI Framework**: JavaFX 17 with Dracula dark theme
- **Build**: Maven with shade plugin for cross-platform JAR execution

### Security
- Master password protected with PBKDF2 array stretching
- In-memory secret handling with `SecureMemory` primitive clearing
- AES-256-GCM authenticated encryption for backup files
- File integrity monitoring via `EnvFileWatcher`
- Session-based `UnlockedVault` with automatic cleanup on lock

### Project Structure
```
src/main/java/com/vault/
├── App.java                          # JavaFX entry point
├── Main.java                         # CLI launcher
├── crypto/                           # Encryption layer
├── db/                               # SQLite persistence
├── engine/                           # Process execution
├── model/                            # Domain objects
├── scanner/                          # File discovery
├── ui/                               # JavaFX controllers
└── util/                             # File management utilities
```
