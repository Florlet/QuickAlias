package com.florlet.quickalias.fabric.mixin;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.QuickAliasLogger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
     * Intercepts the end of the {@link ClientLanguage#loadFrom} method (before the ImmutableMap is created).
     * <p>
     * Captures the local variable map being built and manually injects the content of the mod's internal language files into it.
     *
     * @param resourceManager The resource manager
     * @param definitions     List of currently enabled languages (e.g., ["en_us", "zh_cn"])
     * @param p_265725_       Obfuscated boolean parameter
     * @param cir             Callback info
     * @param map             Local variable capture: The language key-value Map currently being built
     */
    @Inject (method = "loadFrom", at = @At (value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;", remap = false))
    private static void quickalias$injectMissingTranslations(ResourceManager resourceManager, List<String> definitions,
                                                             boolean p_265725_,
                                                             CallbackInfoReturnable<ClientLanguage> cir,
                                                             @Local Map<String, String> map) {
        // Check if Fabric API is installed
        if (FabricLoader.getInstance().isModLoaded("fabric-api")) {
            return;
        }

        // If Fabric API is not installed, start manual injection process
        for (String langCode : definitions) {
            String path = String.format("/assets/%s/lang/%s.json", QuickAliasClient.MOD_ID, langCode);

            // Try to read the language file directly from the mod Jar's classpath
            try (InputStream stream = FabricLanguageMixin.class.getResourceAsStream(path)) {
                if (stream != null) {
                    QuickAliasLogger.info("Manually injecting language file: " + path);

                    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        JsonObject json = quickalias$GSON.fromJson(reader, JsonObject.class);

                        // Put key-value pairs from JSON into the game's language Map one by one
                        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue().getAsString();

                            // Use 'put' to simulate vanilla loading behavior
                            map.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                QuickAliasLogger.error("Failed to inject language file: " + path + "\n" + e);
            }
        }
    }
}
