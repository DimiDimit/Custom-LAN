package com.dimitrodam.customlan.command.argument;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.dimitrodam.customlan.TunnelType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class TunnelArgumentType implements ArgumentType<TunnelType> {
    public static final DynamicCommandExceptionType INVALID_TUNNEL_EXCEPTION = new DynamicCommandExceptionType(
            tunnel -> Text.translatable("argument.tunnel.invalid", tunnel));

    private TunnelArgumentType() {
    }

    public static TunnelArgumentType tunnel() {
        return new TunnelArgumentType();
    }

    public static TunnelType getTunnel(CommandContext<ServerCommandSource> context, String name) {
        return context.getArgument(name, TunnelType.class);
    }

    @Override
    public TunnelType parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readUnquotedString();
        TunnelType tunnelType = TunnelType.byName(string, null);
        if (tunnelType == null) {
            throw INVALID_TUNNEL_EXCEPTION.create(string);
        }
        return tunnelType;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(TunnelType.values()).map(tunnelType -> tunnelType.getName()),
                builder);
    }
}
