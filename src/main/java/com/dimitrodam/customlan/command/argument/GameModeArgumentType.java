package com.dimitrodam.customlan.command.argument;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

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
import net.minecraft.world.GameMode;

public class GameModeArgumentType implements ArgumentType<GameMode> {
    private static final Collection<String> EXAMPLES = Arrays.asList("survival", "creative");
    public static final DynamicCommandExceptionType INVALID_GAMEMODE_EXCEPTION = new DynamicCommandExceptionType(
            gameMode -> Text.translatable("argument.gameMode.invalid", gameMode));

    private GameModeArgumentType() {
    }

    public static GameModeArgumentType gameMode() {
        return new GameModeArgumentType();
    }

    public static GameMode getGameMode(CommandContext<ServerCommandSource> context, String name) {
        return context.getArgument(name, GameMode.class);
    }

    @Override
    public GameMode parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readUnquotedString();
        GameMode gameMode = GameMode.byName(string, null);
        if (gameMode == null) {
            throw INVALID_GAMEMODE_EXCEPTION.create(string);
        }
        return gameMode;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(GameMode.values()).map(gameMode -> gameMode.getName()),
                builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
