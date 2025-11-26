package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Global Mod Settings Screen.
 *
 * @author Florlet
 */
public class ModConfigScreen extends Screen {
    private final Screen parent;
    private final ConfigManager.AliasConfig config;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("quickalias.config.title"));
        this.parent = parent;
        this.config = ConfigManager.getInstance().getConfig();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 70;

        // Settings Button Toggle
        this.addRenderableWidget(new FlatButton(centerX - 100, y, 200, 16,
                                                Component.translatable("quickalias.config.show_settings",
                                                                       boolToText(config.settings.showSettingsButton)),
                                                btn -> {
                                                    config.settings.showSettingsButton = !config.settings.showSettingsButton;
                                                    btn.setMessage(
                                                            Component.translatable("quickalias.config.show_settings",
                                                                                   boolToText(
                                                                                           config.settings.showSettingsButton)));
                                                }));

        y += 24;

        // Shortcut Button Toggle
        this.addRenderableWidget(new FlatButton(centerX - 100, y, 200, 16,
                                                Component.translatable("quickalias.config.show_shortcut",
                                                                       boolToText(config.settings.showShortcutButton)),
                                                btn -> {
                                                    config.settings.showShortcutButton = !config.settings.showShortcutButton;
                                                    btn.setMessage(
                                                            Component.translatable("quickalias.config.show_shortcut",
                                                                                   boolToText(
                                                                                           config.settings.showShortcutButton)));
                                                }));

        y += 24;

        // Button Style Toggle
        this.addRenderableWidget(new FlatButton(centerX - 100, y, 200, 16,
                                                Component.translatable("quickalias.config.button_style",
                                                                       getStyleName(config.settings.useFlatStyle)),
                                                btn -> {
                                                    config.settings.useFlatStyle = !config.settings.useFlatStyle;
                                                    // Immediately update the button text
                                                    btn.setMessage(
                                                            Component.translatable("quickalias.config.button_style",
                                                                                   getStyleName(
                                                                                           config.settings.useFlatStyle)));
                                                    // Force config save to persist state immediately
                                                    ConfigManager.getInstance().save();
                                                }));

        y += 24;

        this.addRenderableWidget(
                new FlatButton(centerX - 50, this.height - 40, 100, 16, Component.translatable("gui.done"), btn -> {
                    ConfigManager.getInstance().save();
                    this.minecraft.setScreen(parent);
                }));
    }

    private String boolToText(boolean b) {
        return b ? Component.translatable("quickalias.config.on").getString()
                : Component.translatable("quickalias.config.off").getString();
    }

    private String getStyleName(boolean isFlat) {
        return isFlat ? Component.translatable("quickalias.config.style.flat").getString()
                : Component.translatable("quickalias.config.style.vanilla").getString();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
}
