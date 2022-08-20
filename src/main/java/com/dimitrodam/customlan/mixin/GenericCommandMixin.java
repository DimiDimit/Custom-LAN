package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.server.dedicated.command.BanIpCommand;
import net.minecraft.server.dedicated.command.BanListCommand;
import net.minecraft.server.dedicated.command.DeOpCommand;
import net.minecraft.server.dedicated.command.OpCommand;
import net.minecraft.server.dedicated.command.PardonCommand;
import net.minecraft.server.dedicated.command.PardonIpCommand;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin({ OpCommand.class, DeOpCommand.class, BanCommand.class, BanIpCommand.class, BanListCommand.class,
        PardonCommand.class, PardonIpCommand.class, WhitelistCommand.class })
public class GenericCommandMixin {
    /**
     * Always allow the host to use the {@code /op}, {@code /deop},
     * {@code /ban}, {@code /ban-ip}, {@code /banlist},
     * {@code /pardon}, {@code /pardon-ip} and {@code /whitelist} commands.
     */
    // The regex is a workaround for
    // https://github.com/SpongePowered/Mixin/issues/467.
    @Inject(method = "desc=/^\\(L(?:net\\/minecraft\\/server\\/command\\/ServerCommandSource|net\\/minecraft\\/class_2168);\\)Z$/", at = @At("HEAD"), cancellable = true)
    private static void checkPermissions(ServerCommandSource source, CallbackInfoReturnable<Boolean> ci) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity
                && source.getServer().isHost(((ServerPlayerEntity) entity).getGameProfile())) {
            ci.setReturnValue(true);
        }
    }
}
