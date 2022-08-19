package com.dimitrodam.customlan.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.server.BannedIpList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanIpCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

@Mixin(BanIpCommand.class)
public class BanIpCommandMixin {
    private static final SimpleCommandExceptionType CANNOT_BAN_HOST_EXCEPTION = new SimpleCommandExceptionType(
            Text.translatable("commands.banip.failed.host"));

    @Inject(method = "banIp", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/PlayerManager;getPlayersByIp(Ljava/lang/String;)Ljava/util/List;"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void checkHost(ServerCommandSource source, String targetIp, @Nullable Text reason,
            CallbackInfoReturnable<Integer> ci, BannedIpList bannedIpList, List<ServerPlayerEntity> list)
            throws CommandSyntaxException {
        if (list.stream().anyMatch(target -> source.getServer().isHost(target.getGameProfile()))) {
            throw CANNOT_BAN_HOST_EXCEPTION.create();
        }
    }
}
