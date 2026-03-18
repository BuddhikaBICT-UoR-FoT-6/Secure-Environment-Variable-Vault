package com.vault.ui;

import com.vault.model.UnlockedVault;

/**
 * DashboardController — Handles the main dashboard after unlocking the vault.
 */
public class DashboardController {

    private UnlockedVault session;

    /**
     * Initializes the dashboard session with the decrypted vault context.
     * 
     * @param session the unlocked vault session
     */
    public void initSession(UnlockedVault session) {
        this.session = session;
        System.out.println("[Dashboard] Vault session initialized.");
        // TODO: Populate dashboard with environment variables and allow edits
    }
}
