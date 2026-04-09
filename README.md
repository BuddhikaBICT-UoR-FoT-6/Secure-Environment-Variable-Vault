# Secure Environment Variable Vault (v1.0.0)

## Description

The Secure Environment Variable Vault is a local desktop application designed to solve one significant developer vulnerability: **the persistence of plaintext `.env` files on developer machines**. 

### The Problem
Typically, developers store API keys, database credentials, and secrets in `.env` files. If a developer's machine is compromised, malware can easily scrape these plaintext files. Accidentally committing a `.env` file to source control is also a frequent, catastrophic mistake.

### The Solution
This application securely encrypts these environment variables using **AES-256-GCM**. A master password strengthened via **PBKDF2** (310,000 iterations) protects the vault. 

---

## 🌟 Core Features
- **Repository Management**: Link local Git repositories to manage their environment variables
- **Direct .env Management**: Read, update, and delete environment variables directly from `.env` files without database storage
- **File Locking**: Prevent external modifications to `.env` files during active sessions
- **File Monitoring**: Real-time detection of manual modifications to linked `.env` files
- **Secure Backups**: Create encrypted backups of `.env` files with PBKDF2-derived encryption
- **Recursive Search**: Automatically locate `.env` files anywhere in repository hierarchy

---

## Tech Stack
- **Core Language:** Java 17 (JavaFX UI)
- **Database:** SQLite (Local Persistence)
- **Cryptography:** AES-256-GCM, PBKDF2-HMAC-SHA256
- **Build System:** Maven

---

## File Architecture

```text
src/main/
├── java/com/vault/
│   ├── crypto/                       # Cryptography Layer (AES/PBKDF2)
│   ├── db/                           # Persistence Layer (SQLite)
│   │   ├── GitRepositoryRepository.java # Git metadata storage
│   │   ├── VaultEntryRepository.java    # Encrypted key-value storage
│   │   └── DatabaseManager.java         # SQLite connection manager
│   ├── engine/                       # Execution & Injection Engine
│   ├── scanner/                      # Intelligence Engine
│   │   └── EnvKeyScanner.java        # Parses .env files
│   ├── util/                         # File Management
│   │   ├── EnvFileLocker.java        # File locking mechanism
│   │   └── EnvFileWatcher.java       # Real-time file monitoring
│   ├── model/                        # Domain Models (GitRepo, VaultEntry)
│   └── ui/                           # JavaFX Controllers
│       ├── DashboardController.java  # Main repository view
│       ├── RepoManagerController.java# Repository linking
│       └── VaultEditorController.java# Key-value CRUD
└── resources/com/vault/          # Views and Styles
    ├── dark-theme.css            # Dracula UI Aesthetics
    ├── dashboard.fxml            # Main UI
    ├── repo_manager.fxml         # Repository linking UI
    └── vault_editor.fxml         # Key editor UI
```

---

## MVC Architecture & Data Flow

The application follows a **Model-View-Controller (MVC)** architecture pattern with clean separation of concerns:

1. **View (FXML)**: JavaFX scene graphs defined in FXML files with CSS styling for consistent Dracula aesthetic
2. **Controller (Java)**: Three main controllers handle user interactions:
   - `DashboardController`: Repository listing and navigation
   - `RepoManagerController`: Repository linking and .env file discovery
   - `VaultEditorController`: Environment variable CRUD operations
3. **Model (Objects)**: Domain objects like `GitRepository`, `VaultEntry`, and `UnlockedVault`
4. **Persistence**: SQLite database stores minimal metadata (vault_meta for PBKDF2 salt, git_repositories for linked repos)

**Data Flow for Key Management:**
- **Add**: User enters key-value pair → written directly to `.env` file → backup encrypted copy stored
- **Read**: Load `.env` file recursively from repository → parse KEY=VALUE pairs → display in UI
- **Update**: Modify value in `.env` file → create backup
- **Delete**: Remove line from `.env` file → create backup

**Security Model:**
- Master password encrypted with PBKDF2 (310,000 iterations) + AES-256-GCM
- Environment variables stored in plaintext within `.env` files (application does not encrypt individual keys)
- Backup feature encrypts entire `.env` file content for recovery purposes
- `EnvFileLocker` prevents file modifications during sessions
- `EnvFileWatcher` monitors for unauthorized changes

---

## Getting Started

### Build and Run
```bash
git clone https://github.com/BuddhikaBICT-UoR-FoT-6/Secure-Environment-Variable-Vault.git
mvn clean package
java -jar target/secure-env-vault-1.0.0.jar
```

### Full Installation Guide
For details on building native installers:

📄 **[docs/INSTALLATION.md](docs/INSTALLATION.md)**

---

## License
MIT License - see [LICENSE](LICENSE) for details.
