package com.florlet.quickalias.gui;

import com.florlet.quickalias.QuickAliasLogger;
import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Import Screen with Confirmation Dialog.
 *
 * @author Florlet
 */
public class ImportConfigScreen extends Screen {
    private static final Gson GSON = new Gson();
    private final Screen parent;
    private AliasScrollList previewList;
    private ConfigManager.AliasConfig importedConfig;

    private Component statusMessage;
    private int statusColor = 0xFFFFFFFF;
    private boolean isPreviewVisible = false;

    // Confirmation State
    private boolean showConfirmation = false;
    private int pendingImportMode = -1;
    private Button confirmBtn;
    private Button cancelConfirmBtn;

    // Main UI Buttons
    private Button replaceBtn;
    private Button skipBtn;
    private Button overwriteBtn;
    private Button fileBtn;
    private Button closeBtn;

    public ImportConfigScreen(Screen parent) {
        super(Component.literal(""));
        this.parent = parent;
        this.statusMessage = Component.translatable("quickalias.import.select_file");
    }

    @Override
    protected void init() {
        int contentTop = 40;
        int contentBottom = this.height - 40;

        this.fileBtn = new FlatButton(10, 10, 100, 16, Component.translatable("quickalias.import.select_file"),
                                      btn -> openFilePicker());
        this.addRenderableWidget(this.fileBtn);

        this.previewList = new AliasScrollList(this.minecraft, null, this.width, this.height, contentTop, contentBottom,
                                               24, true);

        if (importedConfig != null && isPreviewVisible) {
            this.previewList.setImportNodes(importedConfig.aliases, getConflicts());
        }
        this.addWidget(this.previewList);

        int bottomY = this.height - 24;
        int btnWidth = 100;
        int gap = 10;
        int startX = (this.width - (btnWidth * 3 + gap * 2)) / 2;

        this.replaceBtn = new FlatButton(startX, bottomY, btnWidth, 16,
                                         Component.translatable("quickalias.import.mode.replace"),
                                         btn -> triggerConfirmation(0));

        this.overwriteBtn = new FlatButton(startX + btnWidth + gap, bottomY, btnWidth, 16,
                                           Component.translatable("quickalias.import.mode.merge_overwrite"),
                                           btn -> triggerConfirmation(2));

        this.skipBtn = new FlatButton(startX + (btnWidth + gap) * 2, bottomY, btnWidth, 16,
                                      Component.translatable("quickalias.import.mode.merge_skip"),
                                      btn -> triggerConfirmation(1));

        // Initial Active State
        updateMainButtonsState();

        this.addRenderableWidget(replaceBtn);
        this.addRenderableWidget(skipBtn);
        this.addRenderableWidget(overwriteBtn);

        this.closeBtn = new FlatButton(this.width - 70, 10, 60, 16, Component.translatable("gui.cancel"),
                                       btn -> this.minecraft.setScreen(parent));
        this.addRenderableWidget(this.closeBtn);

        // Confirmation Widgets (Hidden initially)
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.confirmBtn = new FlatButton(centerX - 105, centerY + 10, 100, 20, Component.translatable("gui.continue"),
                                         btn -> {
                                             executeImport(pendingImportMode);
                                             showConfirmation = false;
                                         });

        this.cancelConfirmBtn = new FlatButton(centerX + 5, centerY + 10, 100, 20, Component.translatable("gui.cancel"),
                                               btn -> {
                                                   showConfirmation = false;
                                                   pendingImportMode = -1;
                                                   updateMainButtonsState();
                                               });

        this.addWidget(confirmBtn);
        this.addWidget(cancelConfirmBtn);
    }

    private void updateMainButtonsState() {
        boolean active = isPreviewVisible && !showConfirmation;
        if (replaceBtn != null) replaceBtn.active = active;
        if (skipBtn != null) skipBtn.active = active;
        if (overwriteBtn != null) overwriteBtn.active = active;
        if (fileBtn != null) fileBtn.active = !showConfirmation;
        if (closeBtn != null) closeBtn.active = !showConfirmation;
    }

    private void triggerConfirmation(int mode) {
        this.pendingImportMode = mode;
        this.showConfirmation = true;
        updateMainButtonsState();
    }

    private void openFilePicker() {
        if (showConfirmation) return;
        CompletableFuture.runAsync(() -> {
            PointerBuffer filters = MemoryUtil.memAllocPointer(1);
            try {
                filters.put(MemoryUtil.memASCII("*.json"));
                filters.flip();
                String result = TinyFileDialogs.tinyfd_openFileDialog(
                        Component.translatable("quickalias.import.select_file").getString(), "", filters, "JSON Files",
                        false);
                if (result != null) {
                    this.minecraft.execute(() -> loadFile(new File(result)));
                }
            } finally {
                MemoryUtil.memFree(filters);
            }
        });
    }

    private void loadFile(File file) {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            ConfigManager.AliasConfig loaded = GSON.fromJson(reader, ConfigManager.AliasConfig.class);
            handleLoadedConfig(loaded);
        } catch (Exception e) {
            QuickAliasLogger.error("Failed to load file", e);
            this.statusMessage = Component.translatable("quickalias.import.error.io_error");
            this.statusColor = 0xFFFF5555;
            this.isPreviewVisible = false;
            this.rebuildWidgets();
        }
    }

    private void handleLoadedConfig(ConfigManager.AliasConfig loaded) {
        this.importedConfig = loaded;

        if (loaded == null || loaded.aliases == null) {
            this.statusMessage = Component.translatable("quickalias.import.error.invalid_json");
            this.statusColor = 0xFFFF5555;
            this.isPreviewVisible = false;
        } else if (loaded.aliases.isEmpty()) {
            this.statusMessage = Component.translatable("quickalias.import.status.valid_zero");
            this.statusColor = 0xFFFFFF55;
            this.isPreviewVisible = false;
        } else {
            this.statusMessage = Component.empty();
            this.isPreviewVisible = true;
            this.previewList.setImportNodes(loaded.aliases, getConflicts());
            this.previewList.setScrollAmount(0);
        }
        updateMainButtonsState();
    }

    private Set<String> getConflicts() {
        if (importedConfig == null) return Collections.emptySet();
        Set<String> localNames = new HashSet<>();
        for (AliasNode node : ConfigManager.getInstance().getConfig().aliases) {
            localNames.add(node.getName().toLowerCase());
        }
        return localNames;
    }

    private void executeImport(int mode) {
        if (importedConfig == null) return;

        ConfigManager.AliasConfig currentConfig = ConfigManager.getInstance().getConfig();
        if (mode == 0) { // Replace
            currentConfig.aliases.clear();
            currentConfig.aliases.addAll(importedConfig.aliases);
        } else { // Merge
            for (AliasNode imported : importedConfig.aliases) {
                boolean exists = currentConfig.aliases.stream()
                        .anyMatch(n -> n.getName().equalsIgnoreCase(imported.getName()));

                if (exists) {
                    if (mode == 2) { // Overwrite
                        currentConfig.aliases.removeIf(n -> n.getName().equalsIgnoreCase(imported.getName()));
                        currentConfig.aliases.add(imported);
                    }
                    // if mode == 1 (Skip), do nothing
                } else {
                    currentConfig.aliases.add(imported);
                }
            }
        }

        ConfigManager.getInstance().save();
        this.minecraft.setScreen(parent);
        if (parent instanceof SettingsScreen) ((SettingsScreen) parent).refreshList();
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean handled) {
        if (showConfirmation) {
            if (confirmBtn.mouseClicked(event, handled) || cancelConfirmBtn.mouseClicked(event, handled)) return true;
            return true; // Consume all clicks to block underlying widgets
        }
        return super.mouseClicked(event, handled);
    }

    private Component getModeComponent(int mode) {
        return switch (mode) {
            case 0 -> Component.translatable("quickalias.import.mode.replace");
            case 1 -> Component.translatable("quickalias.import.mode.merge_skip");
            case 2 -> Component.translatable("quickalias.import.mode.merge_overwrite");
            default -> Component.literal("?");
        };
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (isPreviewVisible) {
            if (this.previewList != null) {
                this.previewList.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            }
        } else {
            guiGraphics.centeredText(this.font, this.statusMessage, this.width / 2, this.height / 2, this.statusColor);
        }

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);

        // Render Confirmation Overlay
        if (showConfirmation) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.fill(0, 0, this.width, this.height, 0x80000000); // Dark overlay

            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int boxW = 240;
            int boxH = 80;
            int boxX = centerX - boxW / 2;
            int boxY = centerY - boxH / 2;

            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF222222);
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFFFFFFFF); // Top border
            guiGraphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0xFFFFFFFF); // Bottom border
            guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFFFFFFFF); // Left border
            guiGraphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, 0xFFFFFFFF); // Right border

            guiGraphics.centeredText(this.font, Component.translatable("quickalias.import.confirm.title"), centerX,
                                     boxY + 10, 0xFFFFFFFF);

            Component modeText = getModeComponent(pendingImportMode);

            Component msg = Component.translatable("quickalias.import.confirm.message", modeText);
            List<FormattedCharSequence> confirmLines = this.font.split(msg, boxW - 10);

            int textY = boxY + 23;
            for (FormattedCharSequence line : confirmLines) {
                guiGraphics.centeredText(this.font, line, centerX, textY, 0xFFAAAAAA);
                textY += 12;
            }

            confirmBtn.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            cancelConfirmBtn.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

            guiGraphics.pose().popMatrix();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }
}
