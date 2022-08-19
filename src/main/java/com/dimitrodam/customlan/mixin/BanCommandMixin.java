package com.dimitrodam.customlan.mixin;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.text.Text;

@Mixin(BanCommand.class)
public class BanCommandMixin {
    private static final SimpleCommandExceptionType CANNOT_BAN_HOST_EXCEPTION = new SimpleCommandExceptionType(
            Text.translatable("commands.ban.failed.host"));

    @ModifyVariable(method = "ban", at = @At("HEAD"), argsOnly = true)
    private static Collection<GameProfile> checkHost(Collection<GameProfile> targets, ServerCommandSource source)
            throws CommandSyntaxException {
        List<GameProfile> newTargets = targets.stream().filter(target -> !source.getServer().isHost(target))
                .collect(Collectors.toList());
        if (newTargets.isEmpty()) {
            throw CANNOT_BAN_HOST_EXCEPTION.create();
        }
        return newTargets;
    }
}
