package com.vault.engine;

import com.vault.crypto.CryptoEngine;
import com.vault.db.VaultEntryRepository;
import com.vault.model.VaultEntry;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ProcessInjector — The heart of the invisible environment injection.
 *
 * It spawns a child developer process (e.g., node, python, mvn).
 * Crucially, it manipulates the environment variables of that process
 * IN MEMORY before it begins. The secrets never touch the disk.
 */
public class ProcessInjector {

    /**
     * Prepares and launches a target command with injected secrets.
     *
     * @param command    The terminal command e.g., "npm run dev", "python
     *                   script.py"
     * @param workingDir The directory where the command should be run
     * @param entries    The list of encrypted VaultEntries for this project
     * @param secretKey  The live AES-256 session key
     * @return A running Process object (used later to capture terminal output)
     * @throws IOException If the target directory or executable doesn't exist
     */
    public Process launch(String command, String workingDir, List<VaultEntry> entries, SecretKey secretKey)
            throws IOException {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Launch command cannot be empty.");
        }

        // 1. Tokenise the command ("npm run dev" -> ["npm", "run", "dev"])
        // We use a regex parser to handle arguments that contain spaces inside quotes.
        List<String> commandTokens = splitCommand(command);

        // 2. Initialise the ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(commandTokens);

        // 3. Set the working directory
        if (workingDir != null && !workingDir.trim().isEmpty()) {
            File dir = new File(workingDir);
            if (!dir.exists() || !dir.isDirectory()) {
                throw new IllegalArgumentException("Working directory does not exist: " + workingDir);
            }
            pb.directory(dir);
        }

        // 4. THE MAGIC HAPPENS HERE
        // pb.environment() returns a live, modifiable Map of the env vars
        // that the child process will inherit. We inject our secrets here.
        Map<String, String> env = pb.environment();

        for (VaultEntry entry : entries) {
            try {
                // Prepare the payload struct using the hex strings from DB
                CryptoEngine.EncryptedPayload payload = new CryptoEngine.EncryptedPayload(
                        VaultEntryRepository.hexToBytes(entry.getIvHex()),
                        VaultEntryRepository.hexToBytes(entry.getCiphertextHex()));

                // Decrypt
                String decryptedValue = CryptoEngine.decryptString(payload, secretKey);

                // Inject into the process map
                env.put(entry.getKeyName(), decryptedValue);

                // For security observability (never log the actual value)
                System.out.println("[Injector] Injected: " + entry.getKeyName());

            } catch (Exception e) {
                // If one variable fails (tampered with), we throw to stop the launch
                throw new RuntimeException("Failed to decrypt variable: " + entry.getKeyName(), e);
            }
        }

        // 5. Combine Error and Standard Output streams into one stream.
        // This makes it much easier to display the process logs in the UI.
        pb.redirectErrorStream(true);

        System.out.println("[Injector] Starting process: " + command);

        // 6. Launch! The OS forks the process with the modified environment map.
        return pb.start();
    }

    /**
     * Utility to split a command line string into a List of arguments.
     * E.g., 'npm run "my script"' turns into -> ["npm", "run", "my script"]
     */
    private List<String> splitCommand(String command) {
        List<String> tokens = new ArrayList<>();
        // Regex matches substrings enclosed in double quotes OR substrings without
        // spaces
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) {
            // Remove the surrounding quotes if they exist
            tokens.add(m.group(1).replace("\"", ""));
        }
        return tokens;
    }
}
