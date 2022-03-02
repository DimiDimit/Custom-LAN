package com.dimitrodam.customlan.mixin;

import com.dimitrodam.customlan.SetCommandsAllowed;
import com.mojang.authlib.GameProfile;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
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
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.WorldSaveHandler;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private MinecraftServer server;
    @Final
    @Shadow
    @Mutable
    private OperatorList ops;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer server, DynamicRegistryManager.Immutable registryManager, WorldSaveHandler saveHandler, int maxPlayers, CallbackInfo ci) {
        this.ops = new OperatorList(
                server.getSavePath(WorldSavePath.ROOT).resolve(PlayerManager.OPERATORS_FILE.getPath()).toFile());

        try {
            this.ops.load();
        } catch (Exception e) {
            LOGGER.warn("Failed to load operators list: ", e);
        }
    }

    @Redirect(method = "addToOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;add(Lnet/minecraft/server/ServerConfigEntry;)V"))
    private void addToOperators(OperatorList ops, ServerConfigEntry<GameProfile> entry, GameProfile profile) {
        if (this.server.isHost(profile)) {
            ((SetCommandsAllowed) this.server.getSaveProperties()).customlan$setCommandsAllowed(true);
        } else {
            ops.add((OperatorEntry) entry);
            this.saveOpList();
        }
    }

    @Redirect(method = "removeFromOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;remove(Ljava/lang/Object;)V"))
    private void removeFromOperators(OperatorList ops, Object entry, GameProfile profile) {
        if (this.server.isHost(profile)) {
            ((SetCommandsAllowed) this.server.getSaveProperties()).customlan$setCommandsAllowed(false);
        } else {
            ops.remove(profile);
            this.saveOpList();
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "isOperator", at = @At("HEAD"), cancellable = true)
    private void isOperator(GameProfile profile, CallbackInfoReturnable<Boolean> ci) {
        if (this.server.isHost(profile)) {
            ci.setReturnValue(this.server.getSaveProperties().areCommandsAllowed());
        } else {
            ci.setReturnValue(((ServerConfigListAccessor<GameProfile>) this.ops).callContains(profile));
        }
    }

    @Inject(method = "getOpNames", at = @At("HEAD"), cancellable = true)
    private void addHostToOpNames(CallbackInfoReturnable<String[]> ci) {
        if (this.server.getSaveProperties().areCommandsAllowed()) {
            ci.setReturnValue(ArrayUtils.insert(0, this.ops.getNames(), this.server.getSinglePlayerName()));
        }
    }

    @Inject(method = "canBypassPlayerLimit", at = @At("HEAD"), cancellable = true)
    private void canBypassPlayerLimit(GameProfile profile, CallbackInfoReturnable<Boolean> ci) {
        if (this.ops.canBypassPlayerLimit(profile)) {
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
