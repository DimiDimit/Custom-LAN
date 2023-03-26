package com.dimitrodam.customlan.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Widget;

@Mixin(GridWidget.class)
public interface GridWidgetAccessor {
    @Accessor
    public List<Widget> getChildren();
}
