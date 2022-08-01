package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;

@Mixin(ButtonWidget.class)
public interface ButtonWidgetAccessor {
    @Accessor
    @Mutable
    public void setOnPress(PressAction onPress);
}
