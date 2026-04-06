package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.AliasNode;
import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editor screen with strict validation logic and dynamic layout.
 *
 * @author Florlet
 */
public class AliasEditorScreen extends Screen {
    private final Screen parent;
    private final AliasNode originalNode;
    private final AliasNode workingNode;
    private final boolean isNewNode;
    private final int depth;

    private EditBox aliasInput;
    private FlatButton saveButton;
    private FlatButton addSubChildBtn;
    private FlatButton addEndNodeBtn;

    // Layout Bounds
    private int cmdAreaTop, cmdAreaBottom;
    private int childAreaTop, childAreaBottom;

    private double commandScrollAmount = 0;
    private double childScrollAmount = 0;

    private final List<AbstractWidget> commandWidgets = new ArrayList<>();
    private final List<AbstractWidget> childWidgets = new ArrayList<>();
    private final List<BreadcrumbSeparator> breadcrumbSeparators = new ArrayList<>();

    private record BreadcrumbSeparator(int x, int y, Component text) {
    }

    private Component errorMessage = null;
    private boolean saved = false;

    // Constants
    private static final int ITEM_HEIGHT = 20;
    private static final int INPUT_HEIGHT = 14;
    private static final int BUTTON_SIZE = 14;
    private static final int MAX_DEPTH = 10;
    private static final int MAX_COMMANDS = 15;

    // Reserved Vars
    private static final Set<String> RESERVED_VARS = Set.of("X", "Y", "Z", "ID", "DIM");

    // Regex: Matches {var} ONLY if NOT preceded by a backslash.
    private static final Pattern VAR_REF_PATTERN = Pattern.compile("(?<!\\\\)\\{([^}]+)}");

    public AliasEditorScreen(Screen parent, AliasNode node, boolean isNewNode) {
        super(Component.translatable("quickalias.editor.title"));
        this.parent = parent;
        this.originalNode = node;
        this.isNewNode = isNewNode;
        if (parent instanceof AliasEditorScreen) {
            this.depth = ((AliasEditorScreen) parent).depth + 1;
        } else {
            this.depth = 1;
        }
        this.workingNode = new AliasNode(node);
        if (this.workingNode.getCommands().isEmpty()) {
            this.workingNode.addCommand("");
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.commandWidgets.clear();
        this.childWidgets.clear();

        // Determine State
        boolean hasChildren = !workingNode.getChildren().isEmpty();
        boolean isMacroMode = workingNode.getCommands().size() > 1;
        boolean isEndNode = workingNode.isEndNode();

        int breadcrumbBottomY = initBreadcrumbs();
        int topMargin = 10;
        int nameInputY = breadcrumbBottomY + topMargin;
        int errorAreaY = nameInputY + INPUT_HEIGHT + 4;
        int errorHeight = 9;
        int cmdAreaStartY = errorAreaY + errorHeight + 4; // Top of Command List

        this.cmdAreaTop = cmdAreaStartY;
        int footerHeight = 35;
        int contentBottom = this.height - footerHeight;

        if (isMacroMode || isEndNode) {
            this.cmdAreaBottom = contentBottom;
            this.childAreaTop = contentBottom;
            this.childAreaBottom = contentBottom;
        } else if (hasChildren) {
            // State: Tree (Has Children) -> 1 Command Row, Expanded children
            int singleCmdHeight = ITEM_HEIGHT + 10;
            this.cmdAreaBottom = this.cmdAreaTop + singleCmdHeight;

            this.childAreaTop = this.cmdAreaBottom + 10; // Start closer to command
            this.childAreaBottom = contentBottom;
        } else {
            // State: Normal -> Split view
            int cmdRows = Math.min(workingNode.getCommands().size() + 1, 5);
            int neededCmdHeight = (cmdRows * ITEM_HEIGHT) + 10;

            // Dynamic split
            int availableHeight = contentBottom - this.cmdAreaTop;
            int actualCmdHeight = Math.min(neededCmdHeight, (int) (availableHeight * 0.6));
            actualCmdHeight = Math.max(actualCmdHeight, ITEM_HEIGHT * 2 + 10);

            this.cmdAreaBottom = this.cmdAreaTop + actualCmdHeight;
            this.childAreaTop = this.cmdAreaBottom + 10;
            this.childAreaBottom = contentBottom;
        }

        // Initialize Widgets
        int centerX = this.width / 2;
        int inputWidth = 200;
        int inputX = centerX - 100;

        this.aliasInput = new EditBox(this.font, inputX, nameInputY, inputWidth, INPUT_HEIGHT + 2,
                                      Component.translatable("quickalias.editor.alias_name"));
        this.aliasInput.setMaxLength(32);
        this.aliasInput.setValue(workingNode.getName());

        // Color logic for Alias Input
        updateNameInputColor();

        // If this is an {END} node, lock the input
        if (isEndNode) {
            this.aliasInput.setEditable(false);
        }

        this.aliasInput.setResponder(val -> {
            // No spaces are allowed in aliases
            if (val.contains(" ")) {
                String filtered = val.replace(" ", "");
                int cursorPos = this.aliasInput.getCursorPosition();

                // Calculate cursor position
                int spacesBeforeCursor = 0;
                if (cursorPos <= val.length()) {
                    spacesBeforeCursor = val.substring(0, cursorPos).length() - val.substring(0, cursorPos)
                            .replace(" ", "").length();
                }

                this.aliasInput.setValue(filtered);
                // Restore cursor position
                this.aliasInput.setCursorPosition(Math.max(0, cursorPos - spacesBeforeCursor));
            }
            workingNode.setName(val);
            updateNameInputColor();
            validate();
        });
        this.addRenderableWidget(this.aliasInput);

        buildCommandsList(isMacroMode, hasChildren);

        // Do not build children list if this is an {END} node or Macro mode
        if (!isMacroMode && !isEndNode) {
            buildChildrenList();
        }

        int bottomY = this.height - 24;
        this.addRenderableWidget(new FlatButton(this.width - 130, bottomY, 60, 16, Component.translatable("gui.cancel"),
                                                btn -> onClose()));
        this.saveButton = new FlatButton(this.width - 65, bottomY, 60, 16, Component.translatable("gui.done"),
                                         btn -> saveAndClose());
        this.addRenderableWidget(this.saveButton);

        validate();
    }

    private void updateNameInputColor() {
        if (workingNode.isEndNode()) {
            this.aliasInput.setTextColor(0xFFAA55FF); // END Purple
        } else if (workingNode.isVariable()) {
            this.aliasInput.setTextColor(0xFFFFAA00); // Variable Orange
        } else {
            this.aliasInput.setTextColor(0xFFFFFFFF);
        }
    }

    private int initBreadcrumbs() {
        this.breadcrumbSeparators.clear();
        int startX = 10;
        int startY = 8;
        int x = startX;
        int y = startY;
        int lineHeight = 18;
        int indent = 10;
        int maxWidth = this.width - 20;

        List<Screen> chain = new ArrayList<>();
        Screen p = this.parent;
        while (p instanceof AliasEditorScreen) {
            chain.add(p);
            p = ((AliasEditorScreen) p).parent;
        }
        chain.add(p);
        Collections.reverse(chain);

        for (Screen s : chain) {
            Component labelComp = (s instanceof AliasEditorScreen) ? Component.literal(
                    ((AliasEditorScreen) s).originalNode.getName().isEmpty() ? "?"
                            : ((AliasEditorScreen) s).originalNode.getName())
                    : Component.translatable("quickalias.editor.nav.home");
            int btnWidth = this.font.width(labelComp) + 10;
            if (x + btnWidth > maxWidth) {
                x = startX + indent;
                y += lineHeight;
            }
            this.addRenderableWidget(new FlatButton(x, y, btnWidth, 14, labelComp, btn -> {
                onClose();
                this.minecraft.setScreen(s);
            }));
            x += btnWidth + 2;
            Component sepText = Component.literal(">");
            int sepWidth = this.font.width(sepText);
            if (x + sepWidth > maxWidth) {
                x = startX + indent;
                y += lineHeight;
            }
            this.breadcrumbSeparators.add(new BreadcrumbSeparator(x, y + 3, sepText));
            x += sepWidth + 5;
        }
        String currentName = workingNode.getName().isEmpty() ? (isNewNode ? "[ ]" : "?") : workingNode.getName();
        Component currentComp = Component.literal(currentName);
        int curWidth = this.font.width(currentComp) + 10;
        if (x + curWidth > maxWidth) {
            x = startX + indent;
            y += lineHeight;
        }
        FlatButton curBtn = new FlatButton(x, y, curWidth, 14, currentComp, btn -> {
        });
        curBtn.active = false;
        this.addRenderableWidget(curBtn);
        return y + lineHeight;
    }

    private void rebuildInterface() {
        this.init();
    }

    private void buildCommandsList(boolean isMacroMode, boolean hasChildren) {
        int currentCmdCount = workingNode.getCommands().size();
        boolean showAddButton = !hasChildren && currentCmdCount < MAX_COMMANDS;

        int contentHeight = (currentCmdCount * ITEM_HEIGHT) + (showAddButton ? ITEM_HEIGHT : 0);
        int viewHeight = cmdAreaBottom - cmdAreaTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        commandScrollAmount = Mth.clamp(commandScrollAmount, 0, maxScroll);
        int startY = (int) (cmdAreaTop - commandScrollAmount);
        int centerX = this.width / 2;

        for (int i = 0; i < workingNode.getCommands().size(); i++) {
            int itemY = startY + (i * ITEM_HEIGHT);
            boolean isVisible = (itemY + ITEM_HEIGHT > cmdAreaTop) && (itemY < cmdAreaBottom);
            String cmd = workingNode.getCommands().get(i);
            EditBox cmdBox = new EditBox(this.font, centerX - 100, itemY + 2, 200, INPUT_HEIGHT,
                                         Component.translatable("quickalias.editor.label.cmd"));
            cmdBox.setMaxLength(256);
            cmdBox.setValue(cmd);
            cmdBox.visible = isVisible;

            // Command Input text always White
            cmdBox.setTextColor(0xFFFFFFFF);

            int idx = i;
            cmdBox.setResponder(val -> {
                if (idx < workingNode.getCommands().size()) {
                    workingNode.getCommands().set(idx, val);
                    validate();
                }
            });
            this.addRenderableWidget(cmdBox);
            this.commandWidgets.add(cmdBox);

            boolean isLastOne = workingNode.getCommands().size() == 1;

            FlatButton delBtn = new FlatButton(centerX + 105, itemY + 2, BUTTON_SIZE, BUTTON_SIZE,
                                               Component.literal("×").withStyle(s -> s.withColor(0xFFFF5555)), 0, 1,
                                               btn -> {
                                                   if (isLastOne && hasChildren) {
                                                       workingNode.getCommands().set(0, "");
                                                       cmdBox.setValue("");
                                                       validate();
                                                   } else if (isLastOne) {
                                                       workingNode.getCommands().set(0, "");
                                                       cmdBox.setValue("");
                                                       validate();
                                                   } else {
                                                       workingNode.getCommands().remove(idx);
                                                       rebuildInterface();
                                                   }
                                               });
            delBtn.visible = isVisible;
            this.addRenderableWidget(delBtn);
            this.commandWidgets.add(delBtn);
        }

        if (showAddButton) {
            int addButtonY = startY + (workingNode.getCommands().size() * ITEM_HEIGHT);
            boolean isVisible = (addButtonY + ITEM_HEIGHT > cmdAreaTop) && (addButtonY < cmdAreaBottom);
            FlatButton addBtn = new FlatButton(centerX - 100, addButtonY + 2, 200, 16,
                                               Component.translatable("quickalias.editor.add_cmd"), btn -> {
                workingNode.addCommand("");
                if (contentHeight + ITEM_HEIGHT > viewHeight)
                    commandScrollAmount = (contentHeight + ITEM_HEIGHT) - viewHeight;
                rebuildInterface();
            });
            addBtn.visible = isVisible;
            this.addRenderableWidget(addBtn);
            this.commandWidgets.add(addBtn);
        }
    }

    private Component formatCommandWithVariables(String cmd) {
        MutableComponent root = Component.empty();
        Matcher m = VAR_REF_PATTERN.matcher(cmd);
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() > lastEnd) {
                root.append(Component.literal(cmd.substring(lastEnd, m.start()))
                                    .withStyle(Style.EMPTY.withColor(0xFFFFFFFF)));
            }
            root.append(Component.literal(m.group()).withStyle(Style.EMPTY.withColor(0xFFFFAA00)));
            lastEnd = m.end();
        }

        if (lastEnd < cmd.length()) {
            root.append(Component.literal(cmd.substring(lastEnd)).withStyle(Style.EMPTY.withColor(0xFFFFFFFF)));
        }

        return root;
    }

    private void buildChildrenList() {
        int contentHeight = (workingNode.getChildren().size() * ITEM_HEIGHT) + ITEM_HEIGHT;
        int viewHeight = childAreaBottom - childAreaTop;
        double maxScroll = Math.max(0, contentHeight - viewHeight);
        childScrollAmount = Mth.clamp(childScrollAmount, 0, maxScroll);
        int startY = (int) (childAreaTop - childScrollAmount);
        int centerX = this.width / 2;

        // Check if any child is a variable
        boolean hasVariableChild = workingNode.getChildren().stream().anyMatch(AliasNode::isVariable);
        // Check if {END} already exists
        boolean hasEndNode = workingNode.getChildren().stream().anyMatch(AliasNode::isEndNode);

        for (int i = 0; i < workingNode.getChildren().size(); i++) {
            int itemY = startY + (i * ITEM_HEIGHT);
            boolean isVisible = (itemY + ITEM_HEIGHT > childAreaTop) && (itemY < childAreaBottom);
            AliasNode child = workingNode.getChildren().get(i);

            String rawLabel = child.getName() + " -> " + (child.getCommands().isEmpty() ? "..."
                    : child.getCommands().getFirst());

            String childName = child.getName();
            String childCmd = child.getCommands().isEmpty() ? "..." : child.getCommands().getFirst();

            // Determine Name Color
            int nameColor = 0xFFFFFFFF;
            if (child.isVariable()) nameColor = 0xFFFFAA00;
            if (child.isEndNode()) nameColor = 0xFFAA55FF;

            // Determine Command Component
            Component cmdComponent = formatCommandWithVariables(childCmd);

            MutableComponent buttonLabel = Component.literal(childName).withStyle(Style.EMPTY.withColor(nameColor))
                    .append(Component.literal(" -> ").withStyle(Style.EMPTY.withColor(0xFF888888)))
                    .append(cmdComponent);

            FlatButton childBtn = new FlatButton(centerX - 100, itemY + 2, 200, INPUT_HEIGHT, buttonLabel,
                                                 btn -> this.minecraft.setScreen(
                                                         new AliasEditorScreen(this, child, false)));
            childBtn.visible = isVisible;
            this.addRenderableWidget(childBtn);
            this.childWidgets.add(childBtn);

            FlatButton delBtn = new FlatButton(centerX + 105, itemY + 2, BUTTON_SIZE, BUTTON_SIZE,
                                               Component.literal("×").withStyle(s -> s.withColor(0xFFFF5555)), 0, 1,
                                               btn -> {
                                                   workingNode.getChildren().remove(child);
                                                   rebuildInterface();
                                               });
            delBtn.visible = isVisible;
            this.addRenderableWidget(delBtn);
            this.childWidgets.add(delBtn);
        }

        int addButtonY = startY + (workingNode.getChildren().size() * ITEM_HEIGHT);
        boolean isVisible = (addButtonY + ITEM_HEIGHT > childAreaTop) && (addButtonY < childAreaBottom);

        if (hasVariableChild) {
            // If a variable child exists, HIDE standard add button.
            // Only show + END button if {END} does not exist yet.
            if (!hasEndNode) {
                Component endLabel = Component.literal("+ ")
                        .append(Component.literal("END").withStyle(Style.EMPTY.withColor(0xFFAA55FF)));

                this.addEndNodeBtn = new FlatButton(centerX - 100, addButtonY + 2, 200, 16, endLabel, btn -> {
                    AliasNode newChild = new AliasNode("{END}");
                    workingNode.addChild(newChild);
                    this.minecraft.setScreen(new AliasEditorScreen(this, newChild, true));
                });
                this.addEndNodeBtn.visible = isVisible;
                this.addRenderableWidget(addEndNodeBtn);
                this.childWidgets.add(addEndNodeBtn);
            }
        } else {
            // Standard "Add Sub-Option" button
            this.addSubChildBtn = new FlatButton(centerX - 100, addButtonY + 2, 200, 16,
                                                 Component.translatable("quickalias.editor.add_sub"), btn -> {
                AliasNode newChild = new AliasNode("");
                workingNode.addChild(newChild);
                this.minecraft.setScreen(new AliasEditorScreen(this, newChild, true));
            });
            this.addSubChildBtn.visible = isVisible;
            this.addRenderableWidget(addSubChildBtn);
            this.childWidgets.add(addSubChildBtn);
        }
    }

    private void validate() {
        errorMessage = null;
        boolean valid = true;
        String name = aliasInput.getValue().trim();

        if (name.isEmpty()) valid = false;

        // Command cannot contain {END} text
        for (String cmd : workingNode.getCommands()) {
            if (cmd.contains("{END}")) {
                errorMessage = Component.translatable("quickalias.editor.error.reserved");
                valid = false;
                break;
            }
        }

        // Root Node Checks
        if (valid && isRootNode()) {
            if (isVariable(name) || "{END}".equals(name)) {
                errorMessage = Component.translatable("quickalias.editor.error.root_variable");
                valid = false;
            }
        }

        // Variable nodes cannot have siblings unless the sibling is {END}.
        if (valid) {
            List<AliasNode> siblings = getSiblings();
            boolean isCurrentVar = isVariable(name);

            if (isCurrentVar) {
                // If I am a variable, my siblings must ONLY be {END} nodes (or me).
                for (AliasNode s : siblings) {
                    if (s != originalNode && !s.isEndNode()) {
                        errorMessage = Component.translatable("quickalias.editor.error.variable_sibling");
                        valid = false;
                        break;
                    }
                }
            } else {
                if (!workingNode.isEndNode()) {
                    for (AliasNode s : siblings) {
                        if (s != originalNode && s.isVariable()) {
                            errorMessage = Component.translatable("quickalias.editor.error.variable_sibling");
                            valid = false;
                            break;
                        }
                    }
                }
            }
        }

        if (valid) {
            List<AliasNode> siblings = getSiblings();
            for (AliasNode n : siblings) {
                if (n != originalNode && n.getName().equalsIgnoreCase(name)) {
                    errorMessage = Component.translatable("quickalias.editor.warning.duplicate");
                    valid = false;
                    break;
                }
            }
        }

        if (valid && RESERVED_VARS.contains(name.toUpperCase())) {
            errorMessage = Component.translatable("quickalias.editor.error.reserved");
            valid = false;
        }

        if (valid && isVariable(name)) {
            String varName = name.substring(1, name.length() - 1);
            if (getParentVariableNames().contains(varName)) {
                errorMessage = Component.translatable("quickalias.editor.error.duplicate_var");
                valid = false;
            }
        }

        if (valid) {
            Set<String> validVars = new HashSet<>();
            validVars.addAll(RESERVED_VARS);
            validVars.addAll(getParentVariableNames());
            if (isVariable(name)) validVars.add(name.substring(1, name.length() - 1));

            for (String cmd : workingNode.getCommands()) {
                Matcher m = VAR_REF_PATTERN.matcher(cmd);
                while (m.find()) {
                    String ref = m.group(1);
                    if (!validVars.contains(ref)) {
                        errorMessage = Component.translatable("quickalias.editor.error.unknown_var", ref);
                        valid = false;
                        break;
                    }
                }
                if (!valid) break;
            }
        }

        if (saveButton != null) saveButton.active = valid;

        if (addSubChildBtn != null) {
            boolean isEnd = workingNode.isEndNode();
            long cmdCount = workingNode.getCommands().size();
            boolean isMacroMode = cmdCount > 1;

            // Check if ANY existing child is a variable
            boolean hasVariableChild = workingNode.getChildren().stream().anyMatch(AliasNode::isVariable);

            /*
              Sub-Child Button Logic:
              Cannot add standard children if:
                  1. Max depth reached
                  2. Current node is End
                  3. Macro Mode (cmds > 1)
                  4. A variable child ALREADY exists (Variables exclude standard siblings)
            */
            addSubChildBtn.active = valid && !isEnd && (depth < MAX_DEPTH) && !isMacroMode && !hasVariableChild;
        }

        if (addEndNodeBtn != null) {
            boolean isEnd = workingNode.isEndNode();
            long cmdCount = workingNode.getCommands().size();
            boolean isMacroMode = cmdCount > 1;

            // + END button is only created if a Variable child exists, so we just check constraints
            addEndNodeBtn.active = valid && !isEnd && (depth < MAX_DEPTH) && !isMacroMode;
        }
    }

    private boolean isRootNode() {
        return depth == 1;
    }

    private boolean isVariable(String s) {
        return s.startsWith("{") && s.endsWith("}") && s.length() > 2 && !"{END}".equals(s);
    }

    private List<AliasNode> getSiblings() {
        if (isRootNode()) return ConfigManager.getInstance().getConfig().aliases;
        if (parent instanceof AliasEditorScreen) return ((AliasEditorScreen) parent).workingNode.getChildren();
        return Collections.emptyList();
    }

    private Set<String> getParentVariableNames() {
        Set<String> vars = new HashSet<>();
        Screen p = this.parent;
        while (p instanceof AliasEditorScreen) {
            AliasNode n = ((AliasEditorScreen) p).workingNode;
            if (n.isVariable()) vars.add(n.getVariableName());
            p = ((AliasEditorScreen) p).parent;
        }
        return vars;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        double scrollSpeed = 15.0;
        int centerX = this.width / 2;
        int areaLeft = centerX - 120;
        int areaRight = centerX + 120;
        if (mouseX >= areaLeft && mouseX <= areaRight) {
            if (mouseY >= cmdAreaTop && mouseY <= cmdAreaBottom) {
                commandScrollAmount -= scrollY * scrollSpeed;
                rebuildInterface();
                return true;
            }
            if (mouseY >= childAreaTop && mouseY <= childAreaBottom) {
                childScrollAmount -= scrollY * scrollSpeed;
                rebuildInterface();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void saveAndClose() {
        if (!saveButton.active) return;
        saved = true;
        workingNode.setName(aliasInput.getValue().trim());
        workingNode.getCommands().removeIf(String::isEmpty);

        if (isNewNode && isRootNode()) ConfigManager.getInstance().getConfig().aliases.add(workingNode);
        else copyNodeData(workingNode, originalNode);
        ConfigManager.getInstance().save();
        if (parent instanceof SettingsScreen) ((SettingsScreen) parent).refreshList();
        if (parent instanceof AliasEditorScreen) ((AliasEditorScreen) parent).rebuildInterface();
        this.minecraft.setScreen(parent);
    }

    private void copyNodeData(AliasNode source, AliasNode target) {
        target.setName(source.getName());
        target.setCommands(new ArrayList<>(source.getCommands()));
        target.setChildren(new ArrayList<>(source.getChildren()));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        for (BreadcrumbSeparator sep : breadcrumbSeparators)
            guiGraphics.text(this.font, sep.text, sep.x, sep.y, 0xFFAAAAAA);

        guiGraphics.enableScissor(0, cmdAreaTop, this.width, cmdAreaBottom);
        for (AbstractWidget widget : this.commandWidgets) widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

        // Only render children area if we are not in Macro Mode and not an END node
        boolean isMacroMode = workingNode.getCommands().size() > 1;
        boolean isEndNode = workingNode.isEndNode();

        if (!isMacroMode && !isEndNode) {
            guiGraphics.enableScissor(0, childAreaTop, this.width, childAreaBottom);
            for (AbstractWidget widget : this.childWidgets) widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.disableScissor();
        }

        int centerX = this.width / 2;
        int inputLeftX = centerX - 100;

        // Define right-align anchor for labels.
        int labelAnchorX = inputLeftX - 15;

        // Render Slash for Root Node
        if (isRootNode()) {
            guiGraphics.pose().pushMatrix();
            float scale = 1.5f;
            guiGraphics.pose().translate(labelAnchorX + 4, aliasInput.getY() + 2);
            guiGraphics.pose().scale(scale, scale);
            guiGraphics.text(this.font, "/", 1, 0, 0xFFAAAAAA);
            guiGraphics.pose().popMatrix();
        }

        // Draw Labels Right-Aligned to labelAnchorX
        Component aliasLabel = Component.translatable("quickalias.editor.label.alias");
        guiGraphics.text(this.font, aliasLabel, labelAnchorX - this.font.width(aliasLabel), aliasInput.getY() + 4,
                         0xFFAAAAAA);

        Component cmdLabel = Component.translatable("quickalias.editor.label.cmd");
        guiGraphics.text(this.font, cmdLabel, labelAnchorX - this.font.width(cmdLabel), cmdAreaTop + 4, 0xFFAAAAAA);

        if (!isMacroMode && !isEndNode) {
            Component subLabel = Component.translatable("quickalias.editor.label.sub_options");
            guiGraphics.text(this.font, subLabel, labelAnchorX - this.font.width(subLabel), childAreaTop + 4,
                             0xFFAAAAAA);
        }

        if (errorMessage != null) {
            int errorY = aliasInput.getY() + INPUT_HEIGHT + 6;
            guiGraphics.text(this.font, errorMessage, centerX - 100, errorY, 0xFFFF5555);
        }
    }

    @Override
    public void onClose() {
        if (!saved && isNewNode && parent instanceof AliasEditorScreen pEditor) {
            pEditor.workingNode.getChildren().remove(this.originalNode);
            pEditor.rebuildInterface();
        }
        this.minecraft.setScreen(parent);
    }
}
