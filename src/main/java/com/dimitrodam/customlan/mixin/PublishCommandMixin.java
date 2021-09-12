package com.dimitrodam.customlan.mixin;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.dimitrodam.customlan.PublishCommandArgumentValues;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.LanServerPinger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.command.PublishCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Mixin(PublishCommand.class)
public class PublishCommandMixin {
    @Shadow
    @Final
    private static SimpleCommandExceptionType FAILED_EXCEPTION;

    private static final DynamicCommandExceptionType PORT_CHANGE_FAILED_EXCEPTION = new DynamicCommandExceptionType(
            oldPort -> new TranslatableText("commands.publish.failed.port_change", new Object[] { oldPort }));
    private static final Text SERVER_STOPPED_TEXT = new TranslatableText("multiplayer.disconnect.server_shutdown");
    private static final String PUBLISH_SUCCESS_TEXT = "commands.publish.success";
    private static final Text PUBLISH_SAVED_TEXT = new TranslatableText("commands.publish.saved");
    private static final Text PUBLISH_STOPPED_TEXT = new TranslatableText("commands.publish.stopped");

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo ci) {
        List<Pair<ArgumentBuilder<ServerCommandSource, ?>, Consumer<PublishCommandArgumentValues>>> arguments = Arrays
                .asList(Pair.of(argument("port", integer(0, 65535)),
                        argumentValues -> argumentValues.getPort = context -> getInteger(context, "port")),
                        Pair.of(argument("onlineMode", bool()),
                                argumentValues -> argumentValues.getOnlineMode = context -> getBool(context,
                                        "onlineMode")),
                        Pair.of(argument("pvpEnabled", bool()),
                                argumentValues -> argumentValues.getPvpEnabled = context -> getBool(context,
                                        "pvpEnabled")),
                        Pair.of(argument("maxPlayers", integer(1, Integer.MAX_VALUE)),
                                argumentValues -> argumentValues.getMaxPlayers = context -> getInteger(context,
                                        "maxPlayers")),
                        Pair.of(argument("motd", greedyString()),
                                argumentValues -> argumentValues.getMotd = context -> getString(context, "motd")));

        Function<PublishCommandArgumentValues, Command<ServerCommandSource>> executeCommand = argumentValues -> context -> execute(
                context.getSource(), argumentValues.getOnlineMode.apply(context),
                argumentValues.getPvpEnabled.apply(context), argumentValues.getPort.apply(context),
                argumentValues.getMaxPlayers.apply(context), argumentValues.getMotd.apply(context));

        PublishCommandArgumentValues argumentValues = new PublishCommandArgumentValues();
        LiteralArgumentBuilder<ServerCommandSource> command = literal("publish")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(executeCommand.apply(new PublishCommandArgumentValues(argumentValues)))
                .then(processArguments(argumentValues, executeCommand, arguments.iterator()))
                .then(literal("stop").executes(context -> stop(context.getSource())));

        dispatcher.register(command);
        ci.cancel();
    }

    private static ArgumentBuilder<ServerCommandSource, ?> processArguments(PublishCommandArgumentValues argumentValues,
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

    private static int execute(ServerCommandSource source, boolean onlineMode, boolean pvpEnabled, int port,
            int maxPlayers, String motd) throws CommandSyntaxException {
        MinecraftServer server = source.getMinecraftServer();
        PlayerManager playerManager = server.getPlayerManager();

        server.setOnlineMode(onlineMode);
        server.setPvpEnabled(pvpEnabled);

        ((PlayerManagerAccessor) playerManager).setMaxPlayers(maxPlayers);

        String oldMotd = server.getServerMotd();
        if (motd != null) {
            server.setMotd(motd);
            // Metadata doesn't get updated automatically.
            server.getServerMetadata().setDescription(new LiteralText(motd));
        }

        if (server.isRemote()) { // Already opened to LAN
            int oldPort = server.getServerPort();
            boolean portChanged = false;
            if (port != oldPort) {
                ServerNetworkIo networkIo = server.getNetworkIo();
                try {
                    networkIo.bind(null, port); // Checks that the port works.
                    networkIo.stop(); // Stops listening on the port, but does not close any existing connections.
                    networkIo.bind(null, port); // Actually starts listening on the new port.
                    ((IntegratedServerAccessor) server).setLanPort(port);
                    portChanged = true;
                } catch (IOException e) {
                    throw PORT_CHANGE_FAILED_EXCEPTION.create(oldPort);
                }
            }

            if (portChanged || (motd != null && !motd.equals(oldMotd))) {
                // Restart the LAN pinger as its properties are immutable.
                ((IntegratedServerAccessor) server).getLanPinger().interrupt();
                try {
                    LanServerPinger lanPinger = new LanServerPinger(motd, Integer.toString(server.getServerPort()));
                    ((IntegratedServerAccessor) server).setLanPinger(lanPinger);
                    lanPinger.start();
                } catch (IOException e) {
                    // The LAN pinger not working isn't the end of the world.
                }
            }

            // The players' permissions may have changed, so send the new command trees.
            for (ServerPlayerEntity player : playerManager.getPlayerList()) {
                playerManager.sendCommandTree(player); // Do not use server.getCommandManager().sendCommandTree(player)
                                                       // directly or things like the gamemode switcher will not update!
            }

            source.sendFeedback(PUBLISH_SAVED_TEXT, true);
        } else {
            if (!server.openToLan(null, false, port)) {
                throw FAILED_EXCEPTION.create();
            }

            source.sendFeedback(new TranslatableText(PUBLISH_SUCCESS_TEXT, new Object[] { port }), true);
        }

        return port;
    }

    private static int stop(ServerCommandSource source) {
        MinecraftServer server = source.getMinecraftServer();

        // Disconnect the connected players.
        UUID localPlayerUuid = ((IntegratedServerAccessor) server).getLocalPlayerUuid();
        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> playerList = new ArrayList<>(playerManager.getPlayerList()); // Needs to be cloned!
        for (ServerPlayerEntity player : playerList) {
            if (!player.getUuid().equals(localPlayerUuid)) {
                player.networkHandler.disconnect(SERVER_STOPPED_TEXT);
            }
        }

        server.getNetworkIo().stop(); // Stops listening on the port, but does not close any existing connections.
        ((IntegratedServerAccessor) server).setLanPort(-1);
        LanServerPinger lanPinger = ((IntegratedServerAccessor) server).getLanPinger();
        if (lanPinger != null) {
            lanPinger.interrupt();
        }

        source.sendFeedback(PUBLISH_STOPPED_TEXT, true);
        return Command.SINGLE_SUCCESS;
    }
}
