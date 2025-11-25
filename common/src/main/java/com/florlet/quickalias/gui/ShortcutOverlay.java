package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import com.florlet.quickalias.core.VariableResolver;
import com.florlet.quickalias.mixin.ChatScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Overlay screen for quick access to aliases via a visual menu.
 *
 * @author Florlet
 */
public class ShortcutOverlay {
    private static final int MAX_LEVELS = 5;
    private static final int MAX_VISIBLE_ROWS = 8;

    // Regex to detect any variable usage {var}
    private static final Pattern ANY_VAR_PATTERN = Pattern.compile("\\{([^}]+)}");
    // System constants that are auto-resolved and don't require user input
    private static final Set<String> SYSTEM_CONSTANTS = Set.of("X", "Y", "Z", "ID", "DIM");

    private final int screenWidth;
    private final int screenHeight;
    private final List<AliasNode> activePath = new ArrayList<>();
    private final int[] levelYCache = new int[MAX_LEVELS + 1];
    private boolean visible = false;
    private int scrollOffsetL1 = 0;

    public ShortcutOverlay(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void toggle() {
        this.visible = !this.visible;
        if (!visible) {
            activePath.clear();
            scrollOffsetL1 = 0;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) return false;

        List<AliasNode> roots = ConfigManager.getInstance().getConfig().aliases;
        if (roots.size() <= MAX_VISIBLE_ROWS) return false;

        if (delta < 0) {
            if (scrollOffsetL1 < roots.size() - MAX_VISIBLE_ROWS) {
                scrollOffsetL1++;
                return true;
            }
        } else if (delta > 0) {
            if (scrollOffsetL1 > 0) {
                scrollOffsetL1--;
                return true;
            }
        }
        return false;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int anchorX, int anchorY) {
        if (!visible) return;

        List<AliasNode> roots = ConfigManager.getInstance().getConfig().aliases;

        // Adjusted Dimensions
        int itemHeight = 16;
        int menuWidth = 80;
        int startYBase = anchorY;

        int visibleCount = Math.min(roots.size(), MAX_VISIBLE_ROWS);
        int l1Height = visibleCount * itemHeight;
        int l1X = anchorX;
        int l1Y = startYBase - l1Height;

        boolean anyHovered = false;

        List<AliasNode> visibleRoots = roots;
        if (roots.size() > MAX_VISIBLE_ROWS) {
            visibleRoots = roots.subList(scrollOffsetL1, scrollOffsetL1 + visibleCount);
        }

        renderMenuLevel(graphics, visibleRoots, l1X, l1Y, menuWidth, itemHeight, mouseX, mouseY);

        if (roots.size() > MAX_VISIBLE_ROWS) {
            renderScrollBar(graphics, l1X + menuWidth - 2, l1Y, l1Height, roots.size(), visibleCount, scrollOffsetL1);
        }

        AliasNode hoveredL1 = getHoveredNode(visibleRoots, l1X, l1Y, menuWidth, itemHeight, mouseX, mouseY);

        if (hoveredL1 != null) {
            updateActivePath(0, hoveredL1);
            anyHovered = true;
        } else if (isMouseInRect(mouseX, mouseY, l1X, l1Y, menuWidth, l1Height)) {
            anyHovered = true;
        }

        for (int i = 0; i < activePath.size(); i++) {
            if (i >= MAX_LEVELS - 1) break;

            AliasNode currentNode = activePath.get(i);
            // If currentNode is null (safety check), skip
            if (currentNode == null) continue;

            if (currentNode.isLeaf()) continue;

            List<AliasNode> children = currentNode.getChildren();
            if (children.isEmpty()) continue;

            List<AliasNode> parentList;
            if (i == 0) parentList = roots;
            else {
                AliasNode parentNode = activePath.get(i - 1);
                // Safety check for parent node
                if (parentNode == null) parentList = Collections.emptyList();
                else parentList = parentNode.getChildren();
            }

            int indexInParent = parentList.indexOf(currentNode);

            if (i == 0 && roots.size() > MAX_VISIBLE_ROWS) {
                indexInParent -= scrollOffsetL1;
            }

            int prevX, prevY;
            if (i == 0) {
                prevX = l1X;
                prevY = l1Y;
            } else {
                prevX = l1X + i * menuWidth;
                prevY = getStoredY(i);
            }

            int currentMenuX = prevX + menuWidth;
            int currentMenuHeight = children.size() * itemHeight;
            int currentMenuY = prevY + (indexInParent * itemHeight);

            if (currentMenuY + currentMenuHeight > screenHeight) {
                currentMenuY = screenHeight - currentMenuHeight;
            }
            if (currentMenuY < 0) currentMenuY = 0;

            storeY(i + 1, currentMenuY);

            renderMenuLevel(graphics, children, currentMenuX, currentMenuY, menuWidth, itemHeight, mouseX, mouseY);

            AliasNode hoveredChild = getHoveredNode(children, currentMenuX, currentMenuY, menuWidth, itemHeight, mouseX,
                                                    mouseY);
            if (hoveredChild != null) {
                updateActivePath(i + 1, hoveredChild);
                anyHovered = true;
            } else if (isMouseInRect(mouseX, mouseY, currentMenuX, currentMenuY, menuWidth, currentMenuHeight)) {
                anyHovered = true;
            }
        }

        if (!anyHovered) {
            activePath.clear();
        }
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y, int height, int total, int visible, int offset) {
        int barHeight = (int) ((float) visible / total * height);
        if (barHeight < 2) barHeight = 2;
        int barY = y + (int) ((float) offset / total * height);

        graphics.fill(x, barY, x + 1, barY + barHeight, 0xFFFFFFFF);
    }

    private void storeY(int levelIndex, int y) {
        levelYCache[levelIndex] = y;
    }

    private int getStoredY(int levelIndex) {
        return levelYCache[levelIndex];
    }

    private void updateActivePath(int levelIndex, AliasNode node) {
        while (activePath.size() <= levelIndex) {
            activePath.add(null);
        }
        activePath.set(levelIndex, node);
        if (activePath.size() > levelIndex + 1) {
            activePath.subList(levelIndex + 1, activePath.size()).clear();
        }
    }

    private boolean isMouseInRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderMenuLevel(GuiGraphics graphics, List<AliasNode> nodes, int x, int y, int w, int h, int mx,
                                 int my) {
        if (nodes == ConfigManager.getInstance().getConfig().aliases && nodes.size() <= MAX_VISIBLE_ROWS) {
            storeY(0, y);
        } else if (nodes == ConfigManager.getInstance().getConfig().aliases) {
            storeY(0, y);
        }

        int totalH = nodes.size() * h;
        int bgColor = 0xF0100010;

        // Background
        graphics.fillGradient(x, y, x + w, y + totalH, bgColor, bgColor);

        // Manual Border
        int borderColor = 0xFFFFFFFF;
        // Top
        graphics.fill(x, y, x + w, y + 1, borderColor);
        // Bottom
        graphics.fill(x, y + totalH - 1, x + w, y + totalH, borderColor);
        // Left
        graphics.fill(x, y, x + 1, y + totalH, borderColor);
        // Right
        graphics.fill(x + w - 1, y, x + w, y + totalH, borderColor);

        for (int i = 0; i < nodes.size(); i++) {
            AliasNode node = nodes.get(i);
            int itemY = y + (i * h);

            boolean isHovered = (mx >= x && mx < x + w && my >= itemY && my < itemY + h);
            boolean isActive = activePath.contains(node);

            if (isHovered || isActive) {
                graphics.fill(x + 1, itemY, x + w - 1, itemY + h, 0x80FFFFFF);
            }

            int color = node.isLeaf() ? 0xFFFFFF : 0x66AAFF;

            // Use node specific colors for variables/constants
            if (node.isVariable()) color = 0xFFAA00;
            if (node.isEndNode()) color = 0xAA55FF;

            String text = node.getName();
            if (!node.isLeaf()) text += " >";

            graphics.drawString(Minecraft.getInstance().font, text, x + 4, itemY + (h - 8) / 2, color);
        }
    }

    private AliasNode getHoveredNode(List<AliasNode> nodes, int x, int y, int w, int h, int mx, int my) {
        if (mx < x || mx >= x + w) return null;
        if (my < y || my >= y + (nodes.size() * h)) return null;

        int index = (my - y) / h;
        if (index >= 0 && index < nodes.size()) {
            return nodes.get(index);
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!activePath.isEmpty()) {
            AliasNode last = activePath.get(activePath.size() - 1);
            // Allow clicking if leaf, end node, OR variable node (for input filling)
            if (last != null && (last.isLeaf() || last.isEndNode() || last.isVariable())) {
                executeNode();
                return true;
            }
        }

        if (visible) {
            this.visible = false;
            activePath.clear();
            return false;
        }
        return false;
    }

    /**
     * <p> Smart Execution Logic.
     * <p> 1. Checks if path contains variable nodes OR commands require arguments.
     * <p> 2. If variables found: Populates chat input with alias path prefix (stopping before variable).
     * <p> 3. If no variables: Resolves context variables (X, Y, Z, ID, DIM) and executes immediately.
     */
    private void executeNode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (activePath.isEmpty()) return;

        // Target is always the last node in the path
        AliasNode target = activePath.get(activePath.size() - 1);
        List<String> targetCommands = target.getCommands();

        // Check if the PATH itself contains any variable nodes
        boolean pathHasVariable = activePath.stream().anyMatch(AliasNode::isVariable);

        // Check if COMMANDS have unresolved variables
        boolean commandNeedsArgs = false;

        // Collect all commands to check for variables
        List<String> allPendingCommands = new ArrayList<>();

        for (AliasNode node : activePath) {
            if (node.getCommands() != null) {
                allPendingCommands.addAll(node.getCommands());
            }
        }

        if (!allPendingCommands.isEmpty()) {
            for (String cmd : allPendingCommands) {
                Matcher m = ANY_VAR_PATTERN.matcher(cmd);
                while (m.find()) {
                    String varName = m.group(1);
                    if (!SYSTEM_CONSTANTS.contains(varName.toUpperCase())) {
                        commandNeedsArgs = true;
                        break;
                    }
                }
                if (commandNeedsArgs) break;
            }
        }

        if (pathHasVariable || commandNeedsArgs) {
            // Fallback: Populate Chat Input for user to type args
            if (mc.screen instanceof ChatScreen) {
                // Construct prefix: All nodes UP TO the first variable node.
                List<String> prefixParts = new ArrayList<>();
                for (AliasNode n : activePath) {
                    if (n.isVariable()) break; // Stop at first variable
                    if (n.isEndNode()) continue;
                    prefixParts.add(n.getName());
                }

                String aliasPath = "/" + String.join(" ", prefixParts) + " ";

                ((ChatScreenAccessor) mc.screen).getInput().setValue(aliasPath);
                ((ChatScreenAccessor) mc.screen).getInput().moveCursorToEnd();
            }
            this.visible = false;
            activePath.clear();
        } else {
            // Direct Execution (Macro Support)
            List<String> accumulatedCommands = new ArrayList<>();

            for (AliasNode node : activePath) {
                if (node.isEndNode()) continue;

                List<String> nodeCmds = node.getCommands();
                if (nodeCmds == null || nodeCmds.isEmpty()) continue;

                if (accumulatedCommands.isEmpty()) {
                    accumulatedCommands.addAll(nodeCmds);
                } else {
                    // Cross-join / Merge logic similar to InputHandler
                    List<String> nextStage = new ArrayList<>();
                    for (String base : accumulatedCommands) {
                        for (String append : nodeCmds) {
                            if (append.isEmpty()) {
                                nextStage.add(base);
                            } else {
                                nextStage.add(base + " " + append);
                            }
                        }
                    }
                    accumulatedCommands = nextStage;
                }
            }

            if (accumulatedCommands.isEmpty()) return;

            for (String cmd : accumulatedCommands) {
                // Resolve ONLY context variables, pass empty Map since no variables captured
                String finalCmd = VariableResolver.resolve(cmd, Collections.emptyMap());

                if (finalCmd.trim().isEmpty()) continue;

                if (finalCmd.startsWith("/")) {
                    mc.player.connection.sendCommand(finalCmd.substring(1));
                } else {
                    mc.player.connection.sendChat(finalCmd);
                }
            }

            this.visible = false;
            activePath.clear();
            mc.setScreen(null);
        }
    }
}
