package com.florlet.quickalias.neoforge;

import com.florlet.quickalias.QuickAliasClient;
import com.florlet.quickalias.gui.SettingsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

@Mod (QuickAliasClient.MOD_ID)
public class QuickAliasNeoForge {
    public QuickAliasNeoForge() {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            QuickAliasClient.init();

            ModLoadingContext.get()
                    .registerExtensionPoint(IConfigScreenFactory.class, QuickAliasConfigScreenFactory::new);
        }
    }

    private static class QuickAliasConfigScreenFactory implements IConfigScreenFactory {
        @Override
        public @NotNull Screen createScreen(@NotNull ModContainer mod, @NotNull Screen parent) {
            return new SettingsScreen(parent);
        }
    }
}
