package com.florlet.quickalias.fabric;

import com.florlet.quickalias.gui.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu Integration for QuickAlias.
 *
 * @author Florlet
 */
public class QuickAliasModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
