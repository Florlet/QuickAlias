package com.florlet.quickalias.fabric.mixin;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.QuickAliasLogger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Manually injects the mod's language files when Fabric API is not installed.
 *
 * @author Florlet
 */
@Mixin (ClientLanguage.class)
public class FabricLanguageMixin {
    @Unique
    private static final Gson quickalias$GSON = new Gson();

    /**
     * Intercepts the language map during the {@link ClientLanguage#loadFrom} method execution.
     * <p>
     * Captures the mutable map before it becomes immutable and injects QuickAlias translations.
     *
     * @param map             Local variable capture: The language key-value Map currently being built
     * @param resourceManager The resource manager (unused, but required for signature matching)
     * @param definitions     List of currently enabled languages (e.g., ["en_us", "zh_cn"])
     * @param p_265725_       Obfuscated boolean parameter (right-to-left flag)
     * @return The modified language map with QuickAlias translations injected
     */
    @ModifyVariable (method = "loadFrom", at = @At (value = "STORE", ordinal = 0), ordinal = 0)
    private static Map<String, String> quickalias$injectMissingTranslations(Map<String, String> map,
                                                                            ResourceManager resourceManager,
                                                                            List<String> definitions,
                                                                            boolean p_265725_) {

        // Check if Fabric API is installed; if yes, skip manual injection
        if (FabricLoader.getInstance().isModLoaded("fabric-api") || FabricLoader.getInstance()
                .isModLoaded("fabric-language-api-v1")) {
            return map;
        }

        // If Fabric API is not installed, start manual injection process
        QuickAliasLogger.info("Fabric API not detected, manually injecting QuickAlias language files...");

        for (String langCode : definitions) {
            String path = String.format("/assets/%s/lang/%s.json", QuickAliasClient.MOD_ID, langCode);

            // Try to read the language file directly from the mod Jar's classpath
            try (InputStream stream = FabricLanguageMixin.class.getResourceAsStream(path)) {
                if (stream == null) {
                    continue;
                }

                QuickAliasLogger.info("Manually injecting language file: " + path);

                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject json = quickalias$GSON.fromJson(reader, JsonObject.class);

                    // Put key-value pairs from JSON into the game's language Map one by one
                    if (json != null) {
                        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue().getAsString();

                            // Use 'put' instead of 'putIfAbsent' to ensure our translations take precedence
                            map.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                QuickAliasLogger.error("Failed to inject language file: " + path + "\n" + e);
            }
        }

        return map;
    }
}
