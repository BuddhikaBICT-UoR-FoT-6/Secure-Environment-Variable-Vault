# Changelog
All notable changes to the Secure Environment Variable Vault project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

## [1.1.0] - 2026-04-08
_See bottom of file — in progress._

---

## [1.0.0] - 2026-03-26 — **Minimum Viable Product (MVP)**

> This is the stable MVP baseline. The application encrypts and injects environment variables
> in-memory without ever writing plaintext secrets to disk.

### Added
- **Core Cryptography**: Implemented `KeyDerivation` using PBKDF2 array-stretching with heavy iterations.
- **Core Cryptography**: Added `CryptoEngine` for AES-256-GCM ensuring robust authenticated encryption of variables.
- **In-Memory Security**: Added `SecureMemory` primitive clearing and `UnlockedVault` session management to dynamically destroy secret keys when locked.
- **Data Layer Structure**: Setup `DatabaseManager` logic using local `vault.db` SQLite creation and persistent tracking. Added `ProjectRepository`, `VaultEntryRepository`, and `VaultMetaRepository` for strict database CRUD operations.
- **Injection Engine Framework**: Designed and integrated `ProcessInjector` that leverages `java.lang.ProcessBuilder` to execute memory-only variable mapping, eliminating `.env` file disk reliance.
- **User Interface**: Hooked in JavaFX lifecycle methods (`App.java`). Added `master_password` authentication controller mapping and dynamic `dashboard` and `vault_editor` component routing for UI/UX MVC compliance.
- **Dracula Dark Theme**: Added `dark-theme.css` with a professional Dracula-inspired colour palette.
- **Installation & Packaging**: Added `Main.java` launcher wrapper compatible with fat JAR execution via `jpackage` into a native Windows `.exe`.
