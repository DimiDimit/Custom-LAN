package com.dimitrodam.customlan.mixin;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.PublishCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

@Mixin(PublishCommand.class)
public class PublishCommandMixin {
    @Shadow
    @Final
    private static SimpleCommandExceptionType FAILED_EXCEPTION;
    @Shadow
    @Final
    private static DynamicCommandExceptionType ALREADY_PUBLISHED_EXCEPTION;

    private static final boolean DEFAULT_ONLINE_MODE = true;
    private static final boolean DEFAULT_PVP_ENABLED = true;
    private static final int DEFAULT_MAX_PLAYERS = 8;

    @Redirect(method = "register", at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;then(Lcom/mojang/brigadier/builder/ArgumentBuilder;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"))
    private static ArgumentBuilder<ServerCommandSource, LiteralArgumentBuilder<ServerCommandSource>> registerAdvanced(
            LiteralArgumentBuilder<ServerCommandSource> command,
            ArgumentBuilder<ServerCommandSource, RequiredArgumentBuilder<ServerCommandSource, Integer>> commandWithPort) {
        return command.then(commandWithPort.then(argument("cheatsAllowed", bool())
                .executes(context -> executeAdvanced(context.getSource(), getBool(context, "cheatsAllowed"),
                        DEFAULT_ONLINE_MODE, DEFAULT_PVP_ENABLED, getInteger(context, "port"), DEFAULT_MAX_PLAYERS,
                        null))
                .then(argument("onlineMode", bool())
                        .executes(context -> executeAdvanced(context.getSource(), getBool(context, "cheatsAllowed"),
                                getBool(context, "onlineMode"), DEFAULT_PVP_ENABLED, getInteger(context, "port"),
                                DEFAULT_MAX_PLAYERS, null))
                        .then(argument("pvpEnabled", bool()).executes(context -> executeAdvanced(context.getSource(),
                                getBool(context, "cheatsAllowed"), getBool(context, "onlineMode"),
                                getBool(context, "pvpEnabled"), getInteger(context, "port"), DEFAULT_MAX_PLAYERS, null))
                                .then(argument("maxPlayers", integer(1, Integer.MAX_VALUE))
                                        .executes(context -> executeAdvanced(context.getSource(),
                                                getBool(context, "cheatsAllowed"), getBool(context, "onlineMode"),
                                                getBool(context, "pvpEnabled"), getInteger(context, "port"),
                                                getInteger(context, "maxPlayers"), null))
                                        .then(argument("motd", greedyString())
                                                .executes(context -> executeAdvanced(context.getSource(),
                                                        getBool(context, "cheatsAllowed"),
                                                        getBool(context, "onlineMode"), getBool(context, "pvpEnabled"),
                                                        getInteger(context, "port"), getInteger(context, "maxPlayers"),
                                                        getString(context, "motd")))))))));
    }

    private static int executeAdvanced(ServerCommandSource source, boolean cheatsAllowed, boolean onlineMode,
            boolean pvpEnabled, int port, int maxPlayers, String motd) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();

        if (server.isRemote()) {
            throw ALREADY_PUBLISHED_EXCEPTION.create(server.getServerPort());
        }

        server.setOnlineMode(onlineMode);
        server.setPvpEnabled(pvpEnabled);

        ((PlayerManagerAccessor) server.getPlayerManager()).setMaxPlayers(maxPlayers);

        if (motd != null) {
            server.setMotd(motd);
            server.getServerMetadata().setDescription(new LiteralText(server.getServerMotd()));
        }

        if (!server.openToLan(null, cheatsAllowed, port)) {
            throw FAILED_EXCEPTION.create();
        }
        source.sendFeedback(new TranslatableText("commands.publish.success", new Object[] { port }), true);
        return port;
    }
}
