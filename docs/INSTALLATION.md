# Installation & Running Guide (v1.1.0)

## Prerequisites

| Requirement | Version | Download |
|---|---|---|
| Java JDK | 17 or newer | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| Hardware | Webcam | Required for Face ID enrollment/verification |

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
This compiles all Java source files and bundles everything (SQLite, JavaFX, Bouncy Castle, and **OpenCV**) into a single fat JAR at `target/secure-env-vault-1.1.0.jar`.

> [!IMPORTANT]
> Because OpenCV includes native binaries, the build will attempt to download artifacts for your specific OS architecture (e.g., `opencv-windows-x86_64.jar`). Ensure you have an active internet connection.

### Step 3 — Run the Application
```bash
java -jar target/secure-env-vault-1.1.0.jar
```

---

## Biometric Setup (Face ID)

To secure a specific environment variable with your face:
1. Open a workspace and select a secret.
2. Click the **Security** button in the actions column.
3. Choose **Face ID**.
4. A camera preview will appear. Center your face in the frame until the capture is successful.
5. The secret is now locked with your identity. Any future attempt to reveal or launch this key will require a biometric scan.

### Grid-PIN Fallback
If no webcam is detected or face verification fails, you can use the **Grid-PIN fallback**:
1. Choose **Grid PIN** during enrollment.
2. Enter a unique alphanumeric PIN.
3. During unlocking, select the characters in the correct order to regain access.

---

## Option B: Install as a Native Windows EXE

### Step 1 — Build the Fat JAR First
```bash
mvn clean package
```

### Step 2 — Package with jpackage
```powershell
jpackage `
  --type exe `
  --dest target\installer `
  --input target `
  --name "SecureVault" `
  --main-class com.vault.Main `
  --main-jar secure-env-vault-1.1.0.jar `
  --app-version 1.1.0 `
  --description "Secure Environment Variable Vault with Biometric Intelligence" `
  --win-shortcut `
  --win-menu `
  --win-menu-group "SecureVault"
```

---

## v1.1 Advanced Features (First-Time Setup)

1. **Link Git Repo**: On the dashboard, click **🔗 Link Git Repository**. Select a local folder containing your source code.
2. **Scan Environment**: The vault will automatically monitor the `.env` file in that folder and suggest keys for importation.
3. **Usage Tracking**: Click **Usage** on any key to see every file and line number where that variable is referenced in your code.
4. **Change Watcher**: If you manually edit a `.env` file in a linked repository, the vault will alert you of an unauthorized modification.
