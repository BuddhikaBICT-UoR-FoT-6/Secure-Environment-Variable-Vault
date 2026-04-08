# Secure Environment Variable Vault (v1.1.0)

## Description

The Secure Environment Variable Vault is a local desktop application designed to solve one significant developer vulnerability: **the persistence of plaintext `.env` files on developer machines**. 

### The Problem
Typically, developers store API keys, database credentials, and secrets in `.env` files. If a developer's machine is compromised, malware can easily scrape these plaintext files. Accidentally committing a `.env` file to source control is also a frequent, catastrophic mistake.

### The Solution (v1.1.0 Intelligence)
This application securely encrypts these environment variables using **AES-256-GCM**. A master password strengthened via **PBKDF2** (310,000 iterations) protects the vault. 

**Version 1.1.0** introduces **Smart Integration**, allowing you to link your local Git repositories, scan for variable usage in source code, and lock individual secrets with **Face ID biometrics**.

---

## 🌟 New Features in v1.1.0
- **Biometric Face Lock**: Individual secrets can be locked with OpenCV identity matching.
- **Grid-PIN Fallback**: Advanced alphanumeric grid for PIN entry when no webcam is available.
- **Git Repo Intelligence**: Link local repos to auto-identify environment keys.
- **Usage Tracker**: Recursively scan code to find every file and line where a secret is referenced.
- **Shadow .env Watcher**: Real-time detection of manual modifications to linked `.env` files.

---

## Tech Stack
- **Core Language:** Java 17 (JavaFX UI)
- **Computer Vision:** OpenCV / JavaCV (Face Identity Recognition)
- **Database:** SQLite (Local Persistence)
- **Cryptography:** AES-256-GCM, PBKDF2-HMAC-SHA256
- **Build System:** Maven

---

## File Architecture

```text
src/main/
├── java/com/vault/
│   ├── crypto/                       # Cryptography Layer (AES/PBKDF2)
│   ├── db/                           # Persistence Layer (SQLite REPO)
│   │   ├── GitRepositoryRepository.java # NEW: Git metadata storage
│   ├── engine/                       # Execution & Injection Engine
│   ├── scanner/                      # NEW: Intelligence Engine
│   │   ├── EnvKeyScanner.java        # Parses .env files
│   │   ├── KeyChangeWatcher.java     # Real-time NIO monitoring
│   │   └── KeyUsageTracker.java      # Source code regex analysis
│   ├── security/                     # NEW: Biometric Layer
│   │   └── FaceAuthManager.java      # Face enrollment & verification
│   ├── model/                        # Domain Models (Project, Entry, GitRepo)
│   └── ui/                           # JavaFX Controllers
│       ├── RepoManagerController.java# NEW: Linked repo management
│       ├── KeyUsageController.java   # NEW: Usage visualization
│       └── FaceLockController.java   # NEW: Biometric gate
└── resources/com/vault/              # Views and Styles
    ├── dark-theme.css                # Dracula UI Aesthetics
    ├── repo_manager.fxml             # NEW: Git linking UI
    ├── key_usage.fxml                # NEW: Scanner results UI
    └── face_unlock.fxml              # NEW: Face ID / PIN Gate
```

---

## MVC Architecture & Data Flow

The project strictly abides by the **Model-View-Controller (MVC)** architectural pattern.

1. **View (FXML)**: Defines the layout and styles (e.g., `face_unlock.fxml` for biometric security).
2. **Controller (Java)**: Handles logic and verification. `FaceAuthManager` is invoked here to compare camera frames against the enrolled identity.
3. **Model (DAO)**: Data is mapped from the SQLite `vault.db` into objects like `VaultEntry`.

**Smart Scanning Flow:**
- The `EnvKeyScanner` reads `.env` files in a linked repository.
- Identified keys are compared against the DB.
- Results are passed to the `KeyUsageTracker`, which identifies line numbers in `.py`, `.js`, and `.java` files for UI display.

---

## Getting Started

### Build and Run
```bash
git clone https://github.com/BuddhikaBICT-UoR-FoT-6/Secure-Environment-Variable-Vault.git
mvn clean package
java -jar target/secure-env-vault-1.1.0.jar
```

### Full Installation Guide
For details on OpenCV setup, Face ID enrollment, and building native installers:

📄 **[docs/INSTALLATION.md](docs/INSTALLATION.md)**

---

## License
MIT License - see [LICENSE](LICENSE) for details.
