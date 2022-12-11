package com.dimitrodam.customlan.mixin;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;

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

import com.dimitrodam.customlan.CustomLanState;
import com.dimitrodam.customlan.SetCommandsAllowed;
import com.mojang.authlib.GameProfile;

import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.BannedIpList;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.Whitelist;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.WorldSaveHandler;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private MinecraftServer server;
    @Shadow
    @Final
    @Mutable
    private OperatorList ops;
    @Shadow
    @Final
    @Mutable
    private BannedPlayerList bannedProfiles;
    @Shadow
    @Final
    @Mutable
    private BannedIpList bannedIps;
    @Shadow
    @Final
    @Mutable
    private Whitelist whitelist;

    @Shadow
    private boolean whitelistEnabled;

    private File toWorldSpecificFile(File file) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(file.getPath()).toFile();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer server, CombinedDynamicRegistries<ServerDynamicRegistryType> registryManager,
            WorldSaveHandler saveHandler, int maxPlayers, CallbackInfo ci) {
        this.ops = new OperatorList(this.toWorldSpecificFile(PlayerManager.OPERATORS_FILE));
        this.bannedProfiles = new BannedPlayerList(this.toWorldSpecificFile(PlayerManager.BANNED_PLAYERS_FILE));
        this.bannedIps = new BannedIpList(this.toWorldSpecificFile(PlayerManager.BANNED_IPS_FILE));
        this.whitelist = new Whitelist(this.toWorldSpecificFile(PlayerManager.WHITELIST_FILE));

        try {
            this.ops.load();
        } catch (Exception e) {
            LOGGER.warn("Failed to load operators list: ", e);
        }
        try {
            this.bannedProfiles.load();
        } catch (IOException e) {
            LOGGER.warn("Failed to load user banlist: ", e);
        }
        try {
            this.bannedIps.load();
        } catch (IOException e) {
            LOGGER.warn("Failed to load ip banlist: ", e);
        }
        try {
            this.whitelist.load();
        } catch (Exception e) {
            LOGGER.warn("Failed to load white-list: ", e);
        }
    }

    @Inject(method = "checkCanJoin", at = @At("HEAD"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> ci) {
        if (this.server.isHost(profile)) {
            ci.setReturnValue(null);
        }
    }

    @Redirect(method = "addToOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;add(Lnet/minecraft/server/ServerConfigEntry;)V"))
    private void addToOperators(OperatorList ops, ServerConfigEntry<GameProfile> entry, GameProfile profile) {
        if (this.server.isHost(profile)) {
            ((SetCommandsAllowed) this.server.getSaveProperties()).setCommandsAllowed(true);
        } else {
            ops.add((OperatorEntry) entry);
            this.saveOpList();
        }
    }

    @Redirect(method = "removeFromOperators", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/OperatorList;remove(Ljava/lang/Object;)V"))
    private void removeFromOperators(OperatorList ops, Object entry, GameProfile profile) {
        if (this.server.isHost(profile)) {
            ((SetCommandsAllowed) this.server.getSaveProperties()).setCommandsAllowed(false);
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
            ci.setReturnValue(ArrayUtils.insert(0, this.ops.getNames(), this.server.getHostProfile().getName()));
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

    private CustomLanState getCustomLanState() {
        return this.server.getOverworld().getPersistentStateManager().getOrCreate(
                CustomLanState::fromNbt, CustomLanState::new, CustomLanState.CUSTOM_LAN_KEY);
    }

    @Inject(method = "isWhitelistEnabled", at = @At("HEAD"), cancellable = true)
    private void isWhitelistEnabled(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(this.getCustomLanState().getWhitelistEnabled());
    }

    @Inject(method = "setWhitelistEnabled", at = @At("TAIL"))
    private void setWhitelistEnabled(boolean whitelistEnabled, CallbackInfo ci) {
        this.getCustomLanState().setWhitelistEnabled(whitelistEnabled);
    }

    @Inject(method = "isWhitelisted", at = @At("HEAD"))
    private void updateWhitelistEnabled(GameProfile profile, CallbackInfoReturnable<Boolean> ci) {
        this.whitelistEnabled = ((PlayerManager) (Object) this).isWhitelistEnabled();
    }
}
