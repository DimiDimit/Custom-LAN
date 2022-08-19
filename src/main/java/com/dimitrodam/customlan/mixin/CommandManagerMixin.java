package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.server.dedicated.command.BanIpCommand;
import net.minecraft.server.dedicated.command.BanListCommand;
import net.minecraft.server.dedicated.command.DeOpCommand;
import net.minecraft.server.dedicated.command.OpCommand;
import net.minecraft.server.dedicated.command.PardonCommand;
import net.minecraft.server.dedicated.command.PardonIpCommand;
import net.minecraft.server.dedicated.command.WhitelistCommand;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCustomCommands(CommandManager.RegistrationEnvironment environment,
            CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        OpCommand.register(this.dispatcher);
        DeOpCommand.register(this.dispatcher);
        BanCommand.register(this.dispatcher);
        BanIpCommand.register(this.dispatcher);
        BanListCommand.register(this.dispatcher);
        PardonCommand.register(this.dispatcher);
        PardonIpCommand.register(this.dispatcher);
        WhitelistCommand.register(this.dispatcher);
    }
}
