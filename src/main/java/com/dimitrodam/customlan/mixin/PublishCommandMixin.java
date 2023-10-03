package com.dimitrodam.customlan.mixin;

import static com.dimitrodam.customlan.command.argument.GameModeArgumentType.gameMode;
import static com.dimitrodam.customlan.command.argument.GameModeArgumentType.getGameMode;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dimitrodam.customlan.CustomLan;
import com.dimitrodam.customlan.CustomLanConfig;
import com.dimitrodam.customlan.CustomLanState;
import com.dimitrodam.customlan.HasRawMotd;
import com.dimitrodam.customlan.LanSettings;
import com.dimitrodam.customlan.PublishCommandArgumentValues;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.client.util.NetworkUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.PublishCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.GameMode;

@Mixin(PublishCommand.class)
public class PublishCommandMixin {
        @Shadow
        @Final
        private static SimpleCommandExceptionType FAILED_EXCEPTION;

        private static final SimpleCommandExceptionType NOT_STARTED_EXCEPTION = new SimpleCommandExceptionType(
                        Text.translatable("commands.publish.failed.not_started"));
        private static final String PUBLISH_STARTED_CUSTOMLAN_TEXT = "commands.publish.started.customlan";
        private static final String PUBLISH_PORT_CHANGE_FAILED_TEXT = "commands.publish.failed.port_change";
        private static final String PUBLISH_SAVED_TEXT = "commands.publish.saved";
        private static final Text PUBLISH_STOPPED_TEXT = Text.translatable("commands.publish.stopped");

        @Inject(method = "register", at = @At("HEAD"), cancellable = true)
        private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo ci) {
                List<Pair<ArgumentBuilder<ServerCommandSource, ?>, Consumer<PublishCommandArgumentValues>>> arguments = Arrays
                                .asList(Pair.of(argument("port", integer(-1, 65535)),
                                                argumentValues -> argumentValues.getPort = context -> getInteger(
                                                                context, "port")),
                                                Pair.of(argument("onlineMode", bool()),
                                                                argumentValues -> argumentValues.getOnlineMode = context -> getBool(
                                                                                context,
                                                                                "onlineMode")),
                                                Pair.of(argument("pvpEnabled", bool()),
                                                                argumentValues -> argumentValues.getPvpEnabled = context -> getBool(
                                                                                context,
                                                                                "pvpEnabled")),
                                                Pair.of(argument("maxPlayers", integer(1, Integer.MAX_VALUE)),
                                                                argumentValues -> argumentValues.getMaxPlayers = context -> getInteger(
                                                                                context,
                                                                                "maxPlayers")),
                                                Pair.of(argument("defaultGameMode", gameMode()),
                                                                argumentValues -> argumentValues.getGameMode = context -> getGameMode(
                                                                                context,
                                                                                "defaultGameMode")),
                                                Pair.of(argument("motd", greedyString()),
                                                                argumentValues -> argumentValues.getMotd = context -> getString(
                                                                                context, "motd")));

                Function<PublishCommandArgumentValues, Command<ServerCommandSource>> executeCommand = argumentValues -> context -> execute(
                                context.getSource(), argumentValues.getOnlineMode.apply(context),
                                argumentValues.getPvpEnabled.apply(context), argumentValues.getPort.apply(context),
                                argumentValues.getMaxPlayers.apply(context), argumentValues.getGameMode.apply(context),
                                argumentValues.getMotd.apply(context));

                LiteralArgumentBuilder<ServerCommandSource> command = processThisAndArguments(literal("publish")
                                .requires(source -> source.hasPermissionLevel(4)),
                                new PublishCommandArgumentValues(context -> {
                                        MinecraftServer server = context.getSource().getServer();
                                        if (server.isRemote()) {
                                                return new LanSettings(server.getDefaultGameMode(),
                                                                server.isOnlineMode(),
                                                                server.isPvpEnabled(), server.getServerPort(),
                                                                server.getMaxPlayerCount(),
                                                                ((HasRawMotd) server).getRawMotd());
                                        }
                                        CustomLanState customLanState = server.getOverworld()
                                                        .getPersistentStateManager()
                                                        .getOrCreate(CustomLanState::fromNbt, CustomLanState::new,
                                                                        CustomLanState.CUSTOM_LAN_KEY);
                                        if (customLanState.getLanSettings() != null) {
                                                return customLanState.getLanSettings();
                                        } else if (CustomLanConfig.INSTANCE.getLanSettings() != null) {
                                                return CustomLanConfig.INSTANCE.getLanSettings();
                                        }
                                        return null;
                                }), executeCommand, arguments.iterator())
                                .then(processThisAndArguments(literal("perworld"),
                                                new PublishCommandArgumentValues(
                                                                context -> context.getSource().getServer()
                                                                                .getOverworld()
                                                                                .getPersistentStateManager()
                                                                                .getOrCreate(CustomLanState::fromNbt,
                                                                                                CustomLanState::new,
                                                                                                CustomLanState.CUSTOM_LAN_KEY)
                                                                                .getLanSettings()),
                                                executeCommand, arguments.iterator()))
                                .then(processThisAndArguments(literal("global"),
                                                new PublishCommandArgumentValues(
                                                                context -> CustomLanConfig.INSTANCE.getLanSettings()),
                                                executeCommand, arguments.iterator()))
                                .then(processThisAndArguments(literal("system"),
                                                new PublishCommandArgumentValues(
                                                                context -> LanSettings.systemDefaults(
                                                                                context.getSource().getServer())),
                                                executeCommand, arguments.iterator()))
                                .then(literal("stop").executes(context -> stop(context.getSource())));

                dispatcher.register(command);
                ci.cancel();
        }

        private static ArgumentBuilder<ServerCommandSource, ?> processArguments(
                        PublishCommandArgumentValues argumentValues,
                        Function<PublishCommandArgumentValues, Command<ServerCommandSource>> executeCommand,
                        Iterator<Pair<ArgumentBuilder<ServerCommandSource, ?>, Consumer<PublishCommandArgumentValues>>> arguments) {
                Pair<ArgumentBuilder<ServerCommandSource, ?>, Consumer<PublishCommandArgumentValues>> argument = arguments
                                .next();
                argument.getRight().accept(argumentValues);
                ArgumentBuilder<ServerCommandSource, ?> newArgument = argument.getLeft()
                                .executes(executeCommand.apply(new PublishCommandArgumentValues(argumentValues)));
                if (arguments.hasNext()) {
                        newArgument = newArgument.then(processArguments(argumentValues, executeCommand, arguments));
                }
                return newArgument;
        }

        private static <T extends ArgumentBuilder<ServerCommandSource, T>> T processThisAndArguments(
                        T builder, PublishCommandArgumentValues argumentValues,
                        Function<PublishCommandArgumentValues, Command<ServerCommandSource>> executeCommand,
                        Iterator<Pair<ArgumentBuilder<ServerCommandSource, ?>, Consumer<PublishCommandArgumentValues>>> arguments) {
                return builder.executes(executeCommand.apply(argumentValues))
                                .then(processArguments(new PublishCommandArgumentValues(argumentValues), executeCommand,
                                                arguments));
        }

        private static int execute(ServerCommandSource source, boolean onlineMode, boolean pvpEnabled,
                        int rawPort, int maxPlayers, GameMode gameMode, String rawMotd) throws CommandSyntaxException {
                int port = rawPort != -1 ? rawPort : NetworkUtils.findLocalPort();
                try {
                        CustomLan.startOrSaveLan(source.getServer(), gameMode, onlineMode, pvpEnabled,
                                        port, maxPlayers, rawMotd,
                                        motd -> source.sendFeedback(() -> Text.translatable(
                                                        PUBLISH_STARTED_CUSTOMLAN_TEXT,
                                                        Texts.bracketedCopyable(String.valueOf(port)), motd), true),
                                        motd -> source.sendFeedback(() -> Text.translatable(PUBLISH_SAVED_TEXT,
                                                        Texts.bracketedCopyable(String.valueOf(port)), motd), true),
                                        () -> {
                                                throw new RuntimeException(FAILED_EXCEPTION.create());
                                        },
                                        oldPort -> source.sendError(Text.translatable(PUBLISH_PORT_CHANGE_FAILED_TEXT,
                                                        Texts.bracketedCopyable(String.valueOf(oldPort)))));
                } catch (RuntimeException e) {
                        if (e.getCause() instanceof CommandSyntaxException cause) {
                                throw cause;
                        }
                        throw e;
                }

                return port;
        }

        private static int stop(ServerCommandSource source) throws CommandSyntaxException {
                MinecraftServer server = source.getServer();

                if (!server.isRemote()) {
                        throw NOT_STARTED_EXCEPTION.create();
                }

                CustomLan.stopLan(server);

                source.sendFeedback(() -> PUBLISH_STOPPED_TEXT, true);
                return Command.SINGLE_SUCCESS;
        }
}
