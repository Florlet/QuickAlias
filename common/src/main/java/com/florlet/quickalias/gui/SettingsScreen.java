package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Main dashboard for QuickAlias.
 *
 * @author Florlet
 */
public class SettingsScreen extends Screen {
    private final Screen parent;
    private AliasScrollList aliasList;
    private EditBox searchBox;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("quickalias.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Increased header height to accommodate search bar
        int headerHeight = 55;
        int footerHeight = 30;

        int centerX = this.width / 2;

        // Search Box
        this.searchBox = new EditBox(this.font, centerX - 100, 28, 200, 16,
                                     Component.translatable("quickalias.settings.search"));
        this.searchBox.setHint(Component.translatable("quickalias.settings.search"));
        this.searchBox.setResponder(text -> {
            if (this.aliasList != null) {
                this.aliasList.setFilter(text);
            }
        });
        this.addRenderableWidget(this.searchBox);

        // Add Root Node Button [+]
        this.addRenderableWidget(new FlatButton(this.width - 30, 28, 20, 16, Component.literal("+"), (btn) -> {
            AliasNode newNode = new AliasNode();
            this.minecraft.setScreen(new AliasEditorScreen(this, newNode, true));
        }));

        // List
        this.aliasList = new AliasScrollList(this.minecraft, this, this.width, this.height, headerHeight,
                                             this.height - footerHeight,
                                             24); // Increased item height slightly for better look
        this.addRenderableWidget(this.aliasList);

        // Restore filter if re-initializing (e.g. resizing window)
        if (!this.searchBox.getValue().isEmpty()) {
            this.aliasList.setFilter(this.searchBox.getValue());
        }

        // Footer Buttons
        int bottomY = this.height - 24;

        this.addRenderableWidget(
                new FlatButton(10, bottomY, 80, 16, Component.translatable("quickalias.settings.global_config"),
                               (btn) -> this.minecraft.setScreen(new ModConfigScreen(this))));

        this.addRenderableWidget(
                new FlatButton(100, bottomY, 80, 16, Component.translatable("quickalias.settings.import_alias"),
                               (btn) -> this.minecraft.setScreen(new ImportConfigScreen(this))));

        this.addRenderableWidget(new FlatButton(this.width - 70, bottomY, 60, 16, Component.translatable("gui.done"),
                                                (btn) -> this.onClose()));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderDirtBackground(guiGraphics);
        this.aliasList.render(guiGraphics, mouseX, mouseY, partialTick);

        // Center Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        // Author label slight offset
        guiGraphics.drawString(this.font, "By Florlet", 10, 10, 0x555555);

        searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ConfigManager.getInstance().save();
        this.minecraft.setScreen(this.parent);
    }

    public void refreshList() {
        if (this.aliasList != null) {
            // Maintain current search filter when refreshing
            this.aliasList.setFilter(this.searchBox.getValue());
        }
    }
}
