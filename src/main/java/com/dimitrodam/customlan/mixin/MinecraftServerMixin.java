package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dimitrodam.customlan.CustomLanServerValues;
import com.dimitrodam.customlan.TunnelType;
import com.dimitrodam.customlan.TunnelType.TunnelException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements CustomLanServerValues {
    private TunnelType tunnelType = null;
    private Text tunnelText = null;
    private String rawMotd = null;

    @Inject(at = @At("TAIL"), method = "shutdown")
    private void postShutdown(CallbackInfo ci) {
        try {
            this.getTunnelType().stop((MinecraftServer) (Object) this);
        } catch (TunnelException e) {
            e.printStackTrace();
        }
    }

    public TunnelType getTunnelType() {
        return this.tunnelType;
    }

    public void setTunnelType(TunnelType tunnelType) {
        this.tunnelType = tunnelType;
    }

    public Text getTunnelText() {
        return this.tunnelText;
    }

    public void setTunnelText(Text tunnelText) {
        this.tunnelText = tunnelText;
    }

    @Override
    public String getRawMotd() {
        return this.rawMotd;
    }

    @Override
    public void setRawMotd(String rawMotd) {
        this.rawMotd = rawMotd;
    }
}
