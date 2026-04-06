package com.florlet.quickalias.mixin;

import com.florlet.quickalias.config.ConfigManager;
import com.florlet.quickalias.core.InputHandler;
import com.florlet.quickalias.core.SuggestionManager;
import com.florlet.quickalias.gui.FlatButton;
import com.florlet.quickalias.gui.SettingsScreen;
import com.florlet.quickalias.gui.ShortcutOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ChatScreen to inject QuickAlias UI elements.
 *
 * @author Florlet
 */
@Mixin (ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Unique
    private ShortcutOverlay quickAlias$shortcutOverlay;
    @Unique
    private Button quickAlias$qaSettingsButton;
    @Unique
    private Button quickAlias$qaShortcutButton;

    @Unique
    private int quickAlias$qaChatInputX = -1;
    @Unique
    private int quickAlias$qaChatInputY = 0;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject (method = "init", at = @At ("RETURN"), remap = false)
    private void onInit(CallbackInfo ci) {
        // Ensure suggestions are updated/registered when ChatScreen opens.
        SuggestionManager.getInstance().update();

        ConfigManager.AliasConfig config = ConfigManager.getInstance().getConfig();
        EditBox input = ((ChatScreenAccessor) this).getInput();

        int buttonSize = 20;
        int spacing = 2;
        int marginLeft = 2;
        int marginBottom = 2;

        // Total height of the bottom bar (including black background)
        int barHeight = 20;

        // Default height for edit box
        int inputWidgetHeight = 12;

        int barTopY = this.height - barHeight - marginBottom;

        // Keep track of bar Y for rendering background
        this.quickAlias$qaChatInputY = barTopY;

        int currentX = marginLeft;

        if (config.settings.showSettingsButton) {
            this.quickAlias$qaSettingsButton = new FlatButton(currentX, barTopY, buttonSize, buttonSize,
                                                              Component.literal("⚙"), (btn) -> Minecraft.getInstance()
                    .setScreen(new SettingsScreen(this)));
            this.addRenderableWidget(this.quickAlias$qaSettingsButton);
            currentX += buttonSize + spacing;
        }

        if (config.settings.showShortcutButton) {
            this.quickAlias$qaShortcutButton = new FlatButton(currentX, barTopY, buttonSize, buttonSize,
                                                              Component.literal("/"), (btn) -> {
                if (this.quickAlias$shortcutOverlay == null) {
                    this.quickAlias$shortcutOverlay = new ShortcutOverlay(this.width, this.height);
                }
                this.quickAlias$shortcutOverlay.toggle();

                if (!this.quickAlias$shortcutOverlay.isVisible() && input != null) {
                    this.setFocused(input);
                }
            });
            this.addRenderableWidget(this.quickAlias$qaShortcutButton);
            currentX += buttonSize + spacing;
        }

        this.quickAlias$shortcutOverlay = new ShortcutOverlay(this.width, this.height);

        this.quickAlias$qaChatInputX = currentX;

        // Adjust vanilla input box position and size
        if (input != null) {
            int extra = 4;
            ((EditBoxAccessor) input).setHeight(inputWidgetHeight);

            int centeredY = barTopY + (barHeight - inputWidgetHeight + extra) / 2;
            input.setY(centeredY);

            input.setX(this.quickAlias$qaChatInputX + extra);
            input.setWidth(this.width - this.quickAlias$qaChatInputX - 2);
        }
    }

    /**
     * Redirects the background fill render call to adjust the chat bar size.
     */
    @Redirect (method = "extractRenderState", at = @At (value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), remap = false)
    private void quickalias$redirectChatBackgroundFill(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2,
                                                       int color) {
        int modifiedX1 = x1;
        int modifiedY1 = y1;

        // Check if we are rendering the bottom chat bar
        if (y2 >= this.height - 5) {
            if (this.quickAlias$qaChatInputX > 0) {
                modifiedX1 = this.quickAlias$qaChatInputX;
                modifiedY1 = this.quickAlias$qaChatInputY;
            }
        }

        guiGraphics.fill(modifiedX1, modifiedY1, x2, y2, color);
    }

    @Inject (method = "extractRenderState", at = @At ("TAIL"), remap = false)
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick,
                                      CallbackInfo ci) {
        if (this.quickAlias$shortcutOverlay != null && this.quickAlias$shortcutOverlay.isVisible()) {
            int anchorX = (this.quickAlias$qaShortcutButton != null) ? this.quickAlias$qaShortcutButton.getX() : 0;
            int anchorY =
                    (this.quickAlias$qaShortcutButton != null) ? this.quickAlias$qaShortcutButton.getY() : this.height;

            Minecraft mc = Minecraft.getInstance();
            double rawMouseX = mc.mouseHandler.xpos() * (double) mc.getWindow()
                    .getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
            double rawMouseY = mc.mouseHandler.ypos() * (double) mc.getWindow()
                    .getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();

            guiGraphics.pose().pushMatrix();
            this.quickAlias$shortcutOverlay.extractRenderState(guiGraphics, (int) rawMouseX, (int) rawMouseY, anchorX,
                                                               anchorY);
            guiGraphics.pose().popMatrix();
        }
    }

    @Inject (method = "mouseClicked", at = @At ("HEAD"), cancellable = true, remap = false)
    private void onMouseClicked(MouseButtonEvent event, boolean handled, CallbackInfoReturnable<Boolean> cir) {
        if (this.quickAlias$shortcutOverlay != null && this.quickAlias$shortcutOverlay.isVisible()) {
            if (this.quickAlias$shortcutOverlay.mouseClicked(event, handled)) {
                EditBox input = ((ChatScreenAccessor) this).getInput();
                if (input != null) this.setFocused(input);
                cir.setReturnValue(true);
            } else {
                EditBox input = ((ChatScreenAccessor) this).getInput();
                if (input != null) this.setFocused(input);
            }
        }
    }

    @Inject (method = "mouseScrolled", at = @At ("HEAD"), cancellable = true, remap = false)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (this.quickAlias$shortcutOverlay != null && this.quickAlias$shortcutOverlay.isVisible()) {
            if (this.quickAlias$shortcutOverlay.mouseScrolled(mouseX, mouseY, verticalDelta)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject (method = "keyPressed", at = @At ("HEAD"), cancellable = true, remap = false)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.quickAlias$shortcutOverlay != null && this.quickAlias$shortcutOverlay.isVisible()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.quickAlias$shortcutOverlay.toggle();
                EditBox input = ((ChatScreenAccessor) this).getInput();
                if (input != null) this.setFocused(input);
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(true);
        }
    }

    @Inject (method = "handleChatInput", at = @At ("HEAD"), cancellable = true, remap = false)
    private void onHandleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
        if (InputHandler.handleChatInput(message)) {
            ci.cancel();
        }
    }
}
