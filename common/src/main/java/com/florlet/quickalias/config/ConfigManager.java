package com.florlet.quickalias.config;

import com.florlet.quickalias.QuickAliasLogger;
import com.florlet.quickalias.core.SuggestionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <p> Singleton for handling JSON configuration.
 * <p> Manages loading and saving of aliases and global settings.
 *
 * @author Florlet
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "quickalias_config.json";
    public static final String CURRENT_VERSION = "1.1";

    private static ConfigManager INSTANCE;
    private AliasConfig currentConfig;

    private ConfigManager() {
        load();
    }

    public static ConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigManager();
        }
        return INSTANCE;
    }

    /**
     * Loads the configuration from the disk.
     * If the file does not exist or fails to load, a default config is created.
     */
    public void load() {
        try {
            Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
            Path file = configDir.resolve(FILE_NAME);

            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    currentConfig = GSON.fromJson(reader, AliasConfig.class);
                } catch (Exception e) {
                    QuickAliasLogger.error("QuickAlias: Failed to load config, resetting to default.", e);
                    currentConfig = null;
                }
            }
        } catch (Exception e) {
            QuickAliasLogger.error("QuickAlias: Failed to initialize config directory.", e);
        }

        if (currentConfig == null) {
            currentConfig = new AliasConfig();
            save();
        } else {
            migrateIfNeeded();
            // Ensure loaded config is applied to suggestions immediately
            SuggestionManager.getInstance().update();
        }
    }

    /**
     * Handle config migration / default value injection.
     */
    private void migrateIfNeeded() {
        boolean modified = false;

        // Ensure settings object exists
        if (currentConfig.settings == null) {
            currentConfig.settings = new Settings();
            modified = true;
        } else {
            // field-level migration
            if (currentConfig.settings.showSettingsButton == null) {
                currentConfig.settings.showSettingsButton = true;
                modified = true;
            }

            if (currentConfig.settings.showShortcutButton == null) {
                currentConfig.settings.showShortcutButton = true;
                modified = true;
            }

            if (currentConfig.settings.useFlatStyle == null) {
                currentConfig.settings.useFlatStyle = true;
                modified = true;
            }

            if (currentConfig.settings.useVanillaChatInput == null) {
                currentConfig.settings.useVanillaChatInput = false;
                modified = true;
            }
        }

        // Ensure alias list exists
        if (currentConfig.aliases == null) {
            currentConfig.aliases = new ArrayList<>();
            modified = true;
        }

        // Version upgrade
        if (currentConfig.version == null || isOlderVersion(currentConfig.version, CURRENT_VERSION)) {
            currentConfig.version = CURRENT_VERSION;
            modified = true;
        }

        if (modified) {
            save();
        }
    }

    /**
     * Compares two version strings.
     */
    private boolean isOlderVersion(String oldVer, String newVer) {
        String[] oldParts = oldVer.split("\\.");
        String[] newParts = newVer.split("\\.");

        int length = Math.max(oldParts.length, newParts.length);

        for (int i = 0; i < length; i++) {
            int oldPart = i < oldParts.length ? Integer.parseInt(oldParts[i]) : 0;
            int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;

            if (oldPart < newPart) return true;
            if (oldPart > newPart) return false;
        }
        return false;
    }

    /**
     * Saves configuration to disk.
     */
    public void save() {
        if (currentConfig == null) return;
        try {
            Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
            Path file = configDir.resolve(FILE_NAME);

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(currentConfig, writer);
            }

            // Update auto-completions immediately after save
            SuggestionManager.getInstance().update();

        } catch (IOException e) {
            QuickAliasLogger.error("QuickAlias: Failed to save config.", e);
        }
    }

    public AliasConfig getConfig() {
        if (currentConfig == null) {
            load();
        }
        return currentConfig;
    }

    /**
     * Root configuration object matching the JSON structure.
     */
    public static class AliasConfig {
        public String version = CURRENT_VERSION;
        public Settings settings = new Settings();
        public List<AliasNode> aliases = new ArrayList<>();
    }

    /**
     * Global settings wrapper.
     */
    public static class Settings {
        public Boolean showSettingsButton = true;
        public Boolean showShortcutButton = true;
        public Boolean useFlatStyle = true;
        public Boolean useVanillaChatInput = false;
    }
}
