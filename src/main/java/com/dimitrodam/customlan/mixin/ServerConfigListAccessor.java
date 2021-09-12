package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.ServerConfigList;

@Mixin(ServerConfigList.class)
public interface ServerConfigListAccessor<K> {
    @Invoker
    public boolean callContains(K object);
}
