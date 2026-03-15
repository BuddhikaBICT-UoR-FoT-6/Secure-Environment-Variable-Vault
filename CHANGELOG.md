# Changelog
All notable changes to the Secure Environment Variable Vault project will be documented in this file.

## [1.0.0] - 2026-03-15

### Added
- **Core Cryptography**: Implemented `KeyDerivation` using PBKDF2 array-stretching with heavy iterations. 
- **Core Cryptography**: Added `CryptoEngine` for AES-256-GCM ensuring robust authenticated encryption of variables.
- **In-Memory Security**: Added `SecureMemory` primitive clearing and `UnlockedVault` session management to dynamically destroy secret keys when locked.
- **Data Layer Structure**: Setup `DatabaseManager` logic using local `vault.db` SQLite creation and persistent tracking. Added `ProjectRepository`, `VaultEntryRepository`, and `VaultMetaRepository` for strict database CRUD operations.
- **Injection Engine Framework**: Designed and integrated `ProcessInjector` that leverages `java.lang.ProcessBuilder` to execute memory-only variable mapping, eliminating `.env` file disk reliance.
- **User Interface Configuration**: Hooked in JavaFX lifecycle methods (`App.java`). Added `master_password` authentication controller mapping and dynamic `dashboard` and `vault_editor` component routing for UI/UX MVC compliance.
