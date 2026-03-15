# Secure Environment Variable Vault

## Description

The Secure Environment Variable Vault is a local desktop application designed to solve one significant developer vulnerability: **the persistence of plaintext `.env` files on developer machines**. 

### The Problem
Typically, developers store API keys, database credentials, and secrets in `.env` files. If a developer's machine is compromised, bad actors (or malware) can easily scrape these plaintext files, leading to massive data breaches and lateral movement. Accidentally committing a `.env` file to source control is also a frequent, catastrophic mistake.

### The Solution
This application securely encrypts these environment variables using **AES-256-GCM** authenticated encryption. A master password, strengthened via **PBKDF2** (310,000 iterations), guarantees the safety of the vault at rest. 

Crucially, **the application never writes the decrypted secrets to disk**. Using Java's `ProcessBuilder`, the vault dynamically hooks into the process environment map in RAM, injects the secrets into memory, and launches your development server (e.g., `npm run dev`, `python script.py`). When the process dies, the memory is cleared.

---

## Tech Stack
- **Core Language:** Java 17
- **UI Framework:** JavaFX
- **Database Architecture:** SQLite via JDBC (Local File Persistence)
- **Cryptography:** Java Crypto Architecture (`javax.crypto`) - AES-256-GCM, PBKDF2-HMAC-SHA256
- **Build System:** Maven

---

## File Architecture

```text
src/main/
├── java/com/vault/
│   ├── App.java                      # Main JavaFX Entry Point
│   ├── crypto/                       # Cryptography Layer
│   │   ├── CryptoEngine.java         # AES-256-GCM Encryption Logic
│   │   ├── KeyDerivation.java        # PBKDF2 Hash functions
│   │   └── SecureMemory.java         # Array zeroing utility
│   ├── db/                           # Persistence Layer
│   │   ├── DatabaseManager.java      # SQLite connection Singleton
│   │   ├── ProjectRepository.java    # CRUD for Projects
│   │   ├── VaultEntryRepository.java # CRUD for Encrypted Key-Value pairs
│   │   └── VaultMetaRepository.java  # PBKDF2 Salt initialization
│   ├── engine/                       # Execution Engine
│   │   ├── ProcessInjector.java      # In-RAM Process variable injection
│   │   └── ProcessOutputStreamer.java# Async Terminal output capture
│   ├── model/                        # Domain Models
│   │   ├── Project.java              
│   │   ├── UnlockedVault.java        # AES Session management
│   │   └── VaultEntry.java           
│   └── ui/                           # JavaFX Controllers
│       ├── DashboardController.java  
│       ├── MasterPasswordController.java
│       └── VaultEditorController.java
└── resources/com/vault/              # Views and Styles
    ├── dark-theme.css                # Dracula UI Aesthetics
    ├── dashboard.fxml                # Sidebar Workspace
    ├── master_password.fxml          # Login UI
    └── vault_editor.fxml             # Secrets Table
```

---

## MVC Architecture & Data Flow

The project strictly abides by the **Model-View-Controller (MVC)** architectural pattern to separate business logic from the user interface.

- **Views (`.fxml` files)**: Responsible strictly for defining visual elements (inputs, tables, layout). 
- **Controllers (`.java` UI files)**: Intercept button clicks and user inputs from the FXML, routing these actions to the appropriate managers. For example, `MasterPasswordController` takes the string password and routes it to `KeyDerivation`.
- **Model (`.java` domain & repository files)**: Represents structured data like `VaultEntry` (which contains Hex strings mapping to an initialization vector and ciphertext). The DAO (Data Access Object) repositories handle querying this model structure directly through SQLite.

**Execution Flow Example (Launching a Project):**
1. The User clicks "Launch" on the **View** (`vault_editor.fxml`).
2. The **Controller** (`VaultEditorController`) iterates through the list of secrets in the table.
3. The Controller gathers the **Model** (`VaultEntry`) representations and routes them to the `ProcessInjector` Engine.
4. The Engine uses the `Project` configurations and the live `UnlockedVault` secret key to map variables invisibly into RAM before calling `.start()`.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
