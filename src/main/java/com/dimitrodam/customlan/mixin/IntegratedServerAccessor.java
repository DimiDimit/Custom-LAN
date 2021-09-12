package com.dimitrodam.customlan.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.LanServerPinger;
import net.minecraft.server.integrated.IntegratedServer;

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    @Accessor
    public LanServerPinger getLanPinger();

    @Accessor
    public void setLanPinger(LanServerPinger lanPinger);

    @Accessor
    public void setLanPort(int lanPort);

    @Accessor
    public UUID getLocalPlayerUuid();
}
