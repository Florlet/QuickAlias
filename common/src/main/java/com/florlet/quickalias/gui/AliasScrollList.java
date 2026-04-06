package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrollable list widget for displaying aliases.
 *
 * @author Florlet
 */
public class AliasScrollList extends ObjectSelectionList<AliasScrollList.@NotNull AliasEntry> {
    private final SettingsScreen parentScreen;
    private final boolean isImportMode;
    private Set<String> conflictNames = Collections.emptySet();
    private String currentFilter = "";

    // Constants regex for highlighting - Added DIM
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("(?i)\\{(X|Y|Z|ID|DIM)}");

    public AliasScrollList(Minecraft minecraft, SettingsScreen parentScreen, int width, int height, int top, int bottom,
                           int itemHeight) {
        this(minecraft, parentScreen, width, height, top, bottom, itemHeight, false);
    }

    public AliasScrollList(Minecraft minecraft, SettingsScreen parentScreen, int width, int height, int top, int bottom,
                           int itemHeight, boolean isImportMode) {
        super(minecraft, width, bottom - top, top, itemHeight);
        this.parentScreen = parentScreen;
        this.isImportMode = isImportMode;
        if (!isImportMode) {
            this.refreshList();
        }
    }

    public void setFilter(String query) {
        this.currentFilter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        this.refreshList();
    }

    public void refreshList() {
        this.clearEntries();
        List<AliasNode> aliases = ConfigManager.getInstance().getConfig().aliases;

        for (AliasNode node : aliases) {
            if (currentFilter.isEmpty() || matchesRecursive(node, currentFilter)) {
                this.addEntry(new AliasEntry(node));
            }
        }
    }

    private boolean matchesRecursive(AliasNode node, String query) {
        if (node.getName().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (String cmd : node.getCommands()) {
            if (cmd.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (AliasNode child : node.getChildren()) {
            if (matchesRecursive(child, query)) return true;
        }
        return false;
    }

    public void setImportNodes(List<AliasNode> nodes, Set<String> conflicts) {
        this.clearEntries();
        this.conflictNames = conflicts == null ? Collections.emptySet() : conflicts;
        if (nodes != null) {
            for (AliasNode node : nodes) {
                this.addEntry(new AliasEntry(node));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    // Helper to format command string with highlighted constants
    private Component formatCommand(String cmd) {
        MutableComponent root = Component.empty();
        Matcher m = CONSTANT_PATTERN.matcher(cmd);
        int lastEnd = 0;
        while (m.find()) {
            // Text before constant
            if (m.start() > lastEnd) {
                root.append(Component.literal(cmd.substring(lastEnd, m.start()))
                                    .withStyle(Style.EMPTY.withColor(0xFFAAAAAA)));
            }
            // The Constant itself (Purple)
            root.append(Component.literal(m.group()).withStyle(Style.EMPTY.withColor(0xFFAA55FF)));
            lastEnd = m.end();
        }
        // Remaining text
        if (lastEnd < cmd.length()) {
            root.append(Component.literal(cmd.substring(lastEnd)).withStyle(Style.EMPTY.withColor(0xFFAAAAAA)));
        }
        return root;
    }

    public class AliasEntry extends ObjectSelectionList.Entry<@NotNull AliasEntry> {
        private final AliasNode node;
        private final FlatButton upBtn;
        private final FlatButton downBtn;
        private final FlatButton editBtn;
        private final FlatButton deleteBtn;

        public AliasEntry(AliasNode node) {
            this.node = node;

            if (!isImportMode) {
                boolean canSort = currentFilter.isEmpty();
                this.upBtn = new FlatButton(0, 0, 12, 8, Component.empty(), btn -> move(-1));
                this.upBtn.active = canSort;
                this.downBtn = new FlatButton(0, 0, 12, 8, Component.empty(), btn -> move(1));
                this.downBtn.active = canSort;
                this.editBtn = new FlatButton(0, 0, 30, 16, Component.translatable("quickalias.gui.edit"),
                                              btn -> Minecraft.getInstance().setScreen(
                                                      new AliasEditorScreen(AliasScrollList.this.parentScreen, node,
                                                                            false)));
                this.deleteBtn = new FlatButton(0, 0, 16, 16,
                                                Component.literal("×").withStyle(s -> s.withColor(0xFFFF5555)), 0, 1,
                                                btn -> {
                                                    ConfigManager.getInstance().getConfig().aliases.remove(node);
                                                    ConfigManager.getInstance().save();
                                                    AliasScrollList.this.refreshList();
                                                });
            } else {
                this.upBtn = null;
                this.downBtn = null;
                this.editBtn = null;
                this.deleteBtn = null;
            }
        }

        private void move(int dir) {
            if (!currentFilter.isEmpty()) return;
            List<AliasNode> list = ConfigManager.getInstance().getConfig().aliases;
            int index = list.indexOf(node);
            if (index >= 0 && index + dir >= 0 && index + dir < list.size()) {
                Collections.swap(list, index, index + dir);
                ConfigManager.getInstance().save();
                AliasScrollList.this.refreshList();
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered,
                                   float delta) {
            int top = this.getY();
            int left = this.getX();
            int width = getRowWidth();
            int entryHeight = 24;

            int bgTop = top + 2;
            int bgBottom = top + entryHeight - 2;
            guiGraphics.fill(left, bgTop, left + width, bgBottom, 0x15FFFFFF);

            int currentX = left + 4;
            int centerY = bgTop + (bgBottom - bgTop) / 2;

            if (!isImportMode) {
                if (currentFilter.isEmpty()) {
                    int btnHeight = (bgBottom - bgTop) / 2;

                    upBtn.setX(currentX);
                    upBtn.setY(bgTop);
                    upBtn.setHeight(btnHeight);
                    upBtn.extractRenderState(guiGraphics, mouseX, mouseY, delta);

                    guiGraphics.text(Minecraft.getInstance().font, "▲", currentX + 4, bgTop + 2, 0xFFFFFFFF);

                    downBtn.setX(currentX);
                    downBtn.setY(bgTop + btnHeight);
                    downBtn.setHeight(btnHeight);
                    downBtn.extractRenderState(guiGraphics, mouseX, mouseY, delta);

                    guiGraphics.text(Minecraft.getInstance().font, "▼", currentX + 4, bgTop + btnHeight,
                                     0xFFFFFFFF);
                }
                currentX += 16;
            }

            // Name & Color Logic
            String nameText = "/" + node.getName();
            int nameColor = 0xFFFFFFFF;
            if (isImportMode && conflictNames.contains(node.getName().toLowerCase())) {
                nameColor = 0xFFFF5555; // Conflict Red
            } else if (node.isVariable()) {
                nameColor = 0xFFFFAA00; // Variable Orange
            } else if (node.isEndNode()) {
                nameColor = 0xFFAA55FF; // End Purple
            }

            guiGraphics.text(Minecraft.getInstance().font, nameText, currentX, centerY - 4, nameColor);
            int nameWidth = Minecraft.getInstance().font.width(nameText);

            int arrowX = currentX + nameWidth + 5;
            guiGraphics.text(Minecraft.getInstance().font, "->", arrowX, centerY - 4, 0xFF888888);
            int arrowWidth = Minecraft.getInstance().font.width("->");

            int contentX = arrowX + arrowWidth + 5;
            boolean hasCommands = !node.getCommands().isEmpty();
            boolean hasChildren = !node.getChildren().isEmpty();

            if (hasChildren) {
                // Display Children
                String displayStr;
                if (hasCommands) {
                    displayStr = String.join(", ", node.getCommands());
                } else {
                    List<String> childNames = node.getChildren().stream().map(AliasNode::getName).toList();
                    displayStr = "[" + String.join(", ", childNames) + "]";
                }

                // Simply render blue for now, children list usually doesn't have constants to highlight
                int buttonsWidth = isImportMode ? 0 : 55;
                int availableWidth = (left + width) - contentX - buttonsWidth - 5;
                String renderedStr = Minecraft.getInstance().font.plainSubstrByWidth(displayStr, availableWidth);
                if (renderedStr.length() < displayStr.length()) renderedStr += "...";

                guiGraphics.text(Minecraft.getInstance().font, renderedStr, contentX, centerY - 4, 0xFF55AAFF);

            } else {
                // Display Commands with highlighting
                String rawCmds = String.join(", ", node.getCommands());

                // Render the component to support colors
                int buttonsWidth = isImportMode ? 0 : 55;
                int availableWidth = (left + width) - contentX - buttonsWidth - 5;

                formatCommand(rawCmds);

                // Truncate raw string first, then format.
                String truncatedRaw = Minecraft.getInstance().font.plainSubstrByWidth(rawCmds, availableWidth);
                if (truncatedRaw.length() < rawCmds.length()) truncatedRaw += "...";

                Component renderComp = formatCommand(truncatedRaw);
                guiGraphics.text(Minecraft.getInstance().font, renderComp, contentX, centerY - 4, 0xFFFFFFFF);
            }

            if (!isImportMode) {
                int btnY = centerY - 8;

                editBtn.setX(left + width - 55);
                editBtn.setY(btnY);
                editBtn.extractRenderState(guiGraphics, mouseX, mouseY, delta);

                deleteBtn.setX(left + width - 20);
                deleteBtn.setY(btnY);
                deleteBtn.extractRenderState(guiGraphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean handled) {
            if (isImportMode || handled) return false;
            if (currentFilter.isEmpty()) {
                if (upBtn.mouseClicked(event, handled)) return true;
                if (downBtn.mouseClicked(event, handled)) return true;
            }
            if (editBtn.mouseClicked(event, handled)) return true;
            if (deleteBtn.mouseClicked(event, handled)) return true;
            return super.mouseClicked(event, handled);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(node.getName());
        }
    }
}
