# System Diagrams

This document provides visual representations of the Secure Environment Variable Vault system.

## 1. Entity Relationship Diagram (ERD)
The following diagram illustrates the logical data relationships within the vault's SQLite database.

```mermaid
erDiagram
    PROJECT ||--o{ VAULT_ENTRY : contains
    PROJECT ||--o{ GIT_REPOSITORY : monitors
    
    PROJECT {
        int id PK
        string name
        string launch_command
        string working_directory
    }

    VAULT_ENTRY {
        int id PK
        int project_id FK
        string key_name
        string iv_hex
        string ciphertext_hex
        boolean is_locked
        string lock_type
        string lock_data
    }

    GIT_REPOSITORY {
        int id PK
        int project_id FK
        string local_path
        string last_scan
    }

    VAULT_METADATA {
        int id PK
        string master_hash
        string salt_hex
        string pepper_hex
    }
```

---

## 2. Database Table Structure
A detailed view of the physical storage schema used in `vault.db`.

```mermaid
classDiagram
    class projects {
        +INTEGER id PK
        +TEXT name
        +TEXT launch_command
        +TEXT working_directory
    }
    class vault_entries {
        +INTEGER id PK
        +INTEGER project_id FK
        +TEXT key_name
        +TEXT iv_hex
        +TEXT ciphertext_hex
        +BOOLEAN is_locked
        +TEXT lock_type
        +TEXT lock_data
    }
    class git_repositories {
        +INTEGER id PK
        +INTEGER project_id FK
        +TEXT local_path
        +TEXT last_scan
    }
    class vault_metadata {
        +INTEGER id PK
        +TEXT master_hash 
        +TEXT salt_hex
        +TEXT pepper_hex
    }
```

---

## 3. MVC Architecture Flow
This diagram shows how user interactions flow through the system components.

```mermaid
graph TD
    subgraph View_FXML
        V1[dashboard.fxml]
        V2[vault_editor.fxml]
        V3[face_unlock.fxml]
    end

    subgraph Controller_Java
        C1[DashboardController]
        C2[VaultEditorController]
        C3[FaceLockController]
    end

    subgraph Model_Data
        M1[Project]
        M2[VaultEntry]
        M3[GitRepository]
    end

    subgraph Logic_Service
        L1[CryptoEngine]
        L2[FaceAuthManager]
        L3[EnvKeyScanner]
    end

    subgraph Data_Access
        D1[VaultEntryRepository]
        D2[DatabaseManager]
    end

    V1 -- "User Actions" --> C1
    C1 -- "Updates" --> V1
    
    C1 -- "Requests Data" --> D1
    D1 -- "Fetches" --> D2
    D2 -- "Returns SQL" --> D1
    D1 -- "Maps to" --> M1
    D1 -- "Maps to" --> M2

    C2 -- "Decrypts via" --> L1
    C3 -- "Verifies via" --> L2
    
    C2 -- "Populates" --> V2
```
