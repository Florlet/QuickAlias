package com.florlet.quickalias.neoforge;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.gui.SettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.ConfigScreenHandler;

@Mod (QuickAliasClient.MOD_ID)
public class QuickAliasNeoForge {
    public QuickAliasNeoForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            QuickAliasClient.init();

            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                                                           () -> new ConfigScreenHandler.ConfigScreenFactory(
                                                                   (mc, parent) -> new SettingsScreen(parent)));
        }
    }
}
