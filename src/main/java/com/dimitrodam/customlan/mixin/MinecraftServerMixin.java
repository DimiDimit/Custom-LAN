package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.dimitrodam.customlan.HasRawMotd;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements HasRawMotd {
    private String rawMotd = null;

    @Override
    public String getRawMotd() {
        return this.rawMotd;
    }

    @Override
    public void setRawMotd(String rawMotd) {
        this.rawMotd = rawMotd;
    }
}
