package com.dimitrodam.customlan.mixin;

import com.mojang.authlib.GameProfile;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.world.dimension.DimensionType;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private MinecraftServer server;
    @Shadow
    @Mutable
    private OperatorList ops;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer server, int maxPlayers, CallbackInfo ci) {
        this.ops = new OperatorList(
                server.getLevelStorage().resolveFile(server.getLevelName(), PlayerManager.OPERATORS_FILE.getPath()));

        try {
            this.ops.load();
        } catch (Exception e) {
            LOGGER.warn("Failed to load operators list: ", e);
        }
    }

    @Redirect(method = "addToOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;add(Lnet/minecraft/server/ServerConfigEntry;)V"))
    private void addToOperators(OperatorList ops, ServerConfigEntry<GameProfile> entry, GameProfile profile) {
        if (this.server.isOwner(profile)) {
            this.server.getWorld(DimensionType.OVERWORLD).getLevelProperties().setCommandsAllowed(true);
        } else {
            ops.add((OperatorEntry) entry);
            this.saveOpList();
        }
    }

    @Redirect(method = "removeFromOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;remove(Ljava/lang/Object;)V"))
    private void removeFromOperators(OperatorList ops, Object entry, GameProfile profile) {
        if (this.server.isOwner(profile)) {
            this.server.getWorld(DimensionType.OVERWORLD).getLevelProperties().setCommandsAllowed(false);
        } else {
            ops.remove(profile);
            this.saveOpList();
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "isOperator", at = @At("HEAD"), cancellable = true)
    private void isOperator(GameProfile profile, CallbackInfoReturnable<Boolean> ci) {
        if (this.server.isOwner(profile)) {
            ci.setReturnValue(this.server.getWorld(DimensionType.OVERWORLD).getLevelProperties().areCommandsAllowed());
        } else {
            ci.setReturnValue(((ServerConfigListAccessor<GameProfile>) this.ops).callContains(profile));
        }
    }

    @Inject(method = "getOpNames", at = @At("HEAD"), cancellable = true)
    private void addHostToOpNames(CallbackInfoReturnable<String[]> ci) {
        if (this.server.getWorld(DimensionType.OVERWORLD).getLevelProperties().areCommandsAllowed()) {
            ci.setReturnValue(ArrayUtils.add(this.ops.getNames(), 0, this.server.getUserName()));
        }
    }

    @Inject(method = "canBypassPlayerLimit", at = @At("HEAD"), cancellable = true)
    private void canBypassPlayerLimit(GameProfile profile, CallbackInfoReturnable<Boolean> ci) {
        if (this.ops.isOp(profile)) { // This actually checks whether the target player can bypass the player limit.
            ci.setReturnValue(true);
        }
    }

    private void saveOpList() {
        try {
            this.ops.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to save operators list: ", e);
        }
    }
}
