package com.florlet.quickalias.forge;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.gui.SettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod (QuickAliasClient.MOD_ID)
public class QuickAliasForge {
    public QuickAliasForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            QuickAliasClient.init();

            MinecraftForge.registerConfigScreen((mc, parent) -> new SettingsScreen(parent));
        }
    }
}