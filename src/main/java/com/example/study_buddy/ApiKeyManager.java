package com.example.study_buddy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages Google Gemini API keys securely.
 * Checks local settings file first (study_buddy_data/settings.properties),
 * and falls back to GEMINI_API_KEY environment variable.
 */
public class ApiKeyManager {

    private static final String SETTINGS_DIR = "study_buddy_data";
    private static final String SETTINGS_FILE = "study_buddy_data/settings.properties";
    private static final String KEY_GEMINI_API = "gemini_api_key";

    private static String cachedKey = null;

    /**
     * Retrieves the Gemini API key.
     * Priority: Cached memory -> settings.properties file -> GEMINI_API_KEY env var.
     */
    public static synchronized String getApiKey() {
        if (cachedKey != null && !cachedKey.trim().isEmpty()) {
            return cachedKey.trim();
        }

        // Try reading from settings.properties
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
                String val = props.getProperty(KEY_GEMINI_API);
                if (val != null && !val.trim().isEmpty()) {
                    cachedKey = val.trim();
                    return cachedKey;
                }
            } catch (IOException e) {
                System.err.println("[ApiKeyManager] Could not read settings.properties: " + e.getMessage());
            }
        }

        // Fallback to environment variable
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            cachedKey = envKey.trim();
            return cachedKey;
        }

        return "";
    }

    /**
     * Saves the Gemini API key to local storage and updates in-memory cache.
     */
    public static synchronized void setApiKey(String apiKey) {
        cachedKey = (apiKey == null) ? "" : apiKey.trim();
        try {
            Path dir = Paths.get(SETTINGS_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            File file = new File(SETTINGS_FILE);
            Properties props = new Properties();
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                } catch (IOException ignored) {}
            }

            props.setProperty(KEY_GEMINI_API, cachedKey);
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Study Buddy User Preferences");
            }
        } catch (IOException e) {
            System.err.println("[ApiKeyManager] Failed to save API key: " + e.getMessage());
        }
    }

    /**
     * Checks if a valid API key is present.
     */
    public static boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Returns a masked preview of the API key for safe UI display (e.g. AIzaSy...9x1Z).
     */
    public static String getMaskedApiKey() {
        String key = getApiKey();
        if (key == null || key.length() < 8) {
            return "Not Configured";
        }
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}
