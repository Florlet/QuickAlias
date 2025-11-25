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
            // Ensure loaded config is applied to suggestions immediately
            SuggestionManager.getInstance().update();
        }
    }

    /**
     * Saves the current configuration to the disk.
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
        public int version = 1;
        public Settings settings = new Settings();
        public List<AliasNode> aliases = new ArrayList<>();
    }

    /**
     * Wrapper for global settings.
     */
    public static class Settings {
        public boolean showSettingsButton = true;
        public boolean showShortcutButton = true;
        public boolean useFlatStyle = true;
    }
}
