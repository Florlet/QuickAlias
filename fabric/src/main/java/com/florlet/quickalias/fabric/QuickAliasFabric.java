package com.florlet.quickalias.fabric;

import com.florlet.quickalias.QuickAliasClient;
import net.fabricmc.api.ClientModInitializer;

public class QuickAliasFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuickAliasClient.init();
    }
}
