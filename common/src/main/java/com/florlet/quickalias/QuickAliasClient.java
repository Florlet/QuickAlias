package com.florlet.quickalias;

import com.florlet.quickalias.config.ConfigManager;

/**
 * <p> Main Client Entry Point for Common Logic.
 * <p> Called by platform-specific loaders during client initialization.
 *
 * @author Florlet
 */
public class QuickAliasClient {
    public static final String MOD_ID = "quickalias";

    public static void init() {
        QuickAliasLogger.info("Initializing client...");
        ConfigManager.getInstance().load();
    }
}
