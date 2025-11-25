package com.florlet.quickalias.gui;

import com.florlet.quickalias.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Custom button implementation that can toggle between modern flat design and vanilla style.
 *
 * @author Florlet
 */
public class FlatButton extends Button {

    private final int colorNormal = 0x40000000; // Transparent black
    private final int colorHover = 0x80555555;  // Dark Grey
    private final int colorDisabled = 0x20000000; // Faint black for disabled

    public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Check global config for style preference
        boolean useFlat = ConfigManager.getInstance().getConfig().settings.useFlatStyle;

        if (useFlat) {
            renderFlat(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            // Use vanilla rendering logic
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderFlat(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int color = colorNormal;

        if (!this.active) {
            color = colorDisabled;
        } else if (this.isHovered) {
            color = colorHover;
        }

        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        // If the component has a defined color, use it. Otherwise, use default state colors.
        int textColor;
        if (this.getMessage().getStyle().getColor() != null) {
            textColor = this.getMessage().getStyle().getColor().getValue();
            // Dim it slightly if disabled, though usually colored buttons are active
            if (!this.active) {
                textColor = (textColor & 0xFEFEFE) >> 1 | 0xFF000000; // Simple dimming
            }
        } else {
            textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        }

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2,
                                       this.getY() + (this.height - 8) / 2, textColor);
    }

    // Expose protected fields from AbstractWidget for resizing
    public void setHeight(int height) {
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
