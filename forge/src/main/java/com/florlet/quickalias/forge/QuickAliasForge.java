package com.florlet.quickalias.forge;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.gui.SettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod (QuickAliasClient.MOD_ID)
public class QuickAliasForge {
    public QuickAliasForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            QuickAliasClient.init();

            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                                                           () -> new ConfigScreenHandler.ConfigScreenFactory(
                                                                   (mc, parent) -> new SettingsScreen(parent)));
        }
    }
}
