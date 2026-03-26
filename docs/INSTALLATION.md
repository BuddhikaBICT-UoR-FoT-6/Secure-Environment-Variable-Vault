# Installation & Running Guide

## Prerequisites

| Requirement | Version | Download |
|---|---|---|
| Java JDK | 17 or newer | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |

---

## Option A: Run from Source Code

### Step 1 — Clone the Repository
```bash
git clone https://github.com/BuddhikaBICT-UoR-FoT-6/Secure-Environment-Variable-Vault.git
cd Secure-Environment-Variable-Vault
```

### Step 2 — Install Dependencies & Build
```bash
mvn clean package
```
This compiles all Java source files and bundles everything (including SQLite, JavaFX, and Bouncy Castle) into a single fat JAR at `target/secure-env-vault-1.0.0.jar`.

### Step 3 — Run the Application
```bash
java -jar target/secure-env-vault-1.0.0.jar
```

The login window will appear. On first launch, a `vault.db` database file is automatically created at `C:\Users\<YourName>\.envvault\vault.db` (Windows) or `~/.envvault/vault.db` (Linux/Mac).

---

## Option B: Install as a Native Windows EXE

> **Requirements:** JDK 17+ must be installed on your machine to run `jpackage`. The resulting `.exe` bundles the Java runtime, so end-users do NOT need Java installed.

### Step 1 — Build the Fat JAR First
```bash
mvn clean package
```

### Step 2 — Package as a Windows EXE using jpackage

Run the following command from the project root directory:

```bash
jpackage ^
  --type exe ^
  --dest target\installer ^
  --input target ^
  --name "LocalVault" ^
  --main-class com.vault.Main ^
  --main-jar secure-env-vault-1.0.0.jar ^
  --app-version 1.0.0 ^
  --description "Secure Local Environment Variable Vault" ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "LocalVault"
```

> **Note:** On Linux/Mac, replace `^` line continuations with `\` and change `--type exe` to `--type dmg` or `--type deb`.

### Step 3 — Run the Installer
Find the generated installer at:
```
target\installer\LocalVault-1.0.0.exe
```

Double-click it to install LocalVault to your Windows machine. After installation, it will appear in your Start Menu and optionally on your Desktop.

---

## Troubleshooting

### 'mvn' is not recognized as an internal or external command
This happens if Maven is installed but not added to your Windows **PATH** variable.

**The Instant Fix:**
Find where Maven is installed (usually `C:\Program Files\apache-maven-x.x.x\bin`) and use the full path in quotes:
```powershell
& "C:\Path\To\Maven\bin\mvn.cmd" clean package
```

**The Permanent Fix:**
1. Open **Edit the system environment variables**.
2. Click **Environment Variables**.
3. Under **System variables**, select **Path** and click **Edit**.
4. Click **New** and paste the path to your Maven `bin` folder (e.g., `C:\Program Files\apache-maven-3.9.9\bin`).
5. Click OK and **restart your terminal**.

### Application crashes with 'Location is not set'
Ensure you are running the command from the **root directory** of the project so that Java can find the FXML files in the resources path.

---

## First-Time Setup (Both Options)

1. Launch the app — the login screen appears.
2. Enter any master password you choose (this becomes your permanent vault password).
3. Click **Unlock Vault** — the vault generates a cryptographic salt and derives your AES-256 key internally.
4. On the Dashboard, click **New** to create a workspace (e.g., `My Node Project`).
5. Click your new project → Click **+ Add Secret**.
6. Enter a secret in `KEY=VALUE` format (e.g., `DATABASE_URL=postgres://localhost/mydb`).
7. Click **🚀 LAUNCH PROJECT** to inject your secrets into memory and start your dev server!

> **Security Note:** Your master password is **never stored on disk**. If you forget it, the vault contents are cryptographically unrecoverable.
