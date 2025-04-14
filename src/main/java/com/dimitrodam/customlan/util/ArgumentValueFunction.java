package com.dimitrodam.customlan.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.command.ServerCommandSource;

@FunctionalInterface
public interface ArgumentValueFunction<R> {
    R apply(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
}
