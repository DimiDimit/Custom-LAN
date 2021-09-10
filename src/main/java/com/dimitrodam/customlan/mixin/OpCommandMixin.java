package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.DeOpCommand;
import net.minecraft.server.dedicated.command.OpCommand;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin({ OpCommand.class, DeOpCommand.class })
public class OpCommandMixin {
    /**
     * Always allow the host to use the {@code /op} and {@code /deop} commands.
     */
    @Inject(method = { "method_13470(Lnet/minecraft/server/command/ServerCommandSource;)Z",
            "method_13147(Lnet/minecraft/server/command/ServerCommandSource;)Z" }, at = @At("HEAD"), cancellable = true)
    private static void checkPermissions(ServerCommandSource source, CallbackInfoReturnable<Boolean> ci) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity
                && source.getServer().isHost(((ServerPlayerEntity) entity).getGameProfile())) {
            ci.setReturnValue(true);
        }
    }
}
