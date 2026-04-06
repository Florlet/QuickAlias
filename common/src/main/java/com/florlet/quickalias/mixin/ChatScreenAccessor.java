package com.florlet.quickalias.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin (ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor (value = "input", remap = false)
    EditBox getInput();
}
