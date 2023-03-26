package com.dimitrodam.customlan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.apache.commons.text.StringSubstitutor;

import com.dimitrodam.customlan.command.argument.GameModeArgumentType;
import com.dimitrodam.customlan.mixin.IntegratedServerAccessor;
import com.dimitrodam.customlan.mixin.PlayerManagerAccessor;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.LanServerPinger;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.dedicated.command.BanCommand;
import net.minecraft.server.dedicated.command.BanIpCommand;
import net.minecraft.server.dedicated.command.BanListCommand;
import net.minecraft.server.dedicated.command.DeOpCommand;
import net.minecraft.server.dedicated.command.OpCommand;
import net.minecraft.server.dedicated.command.PardonCommand;
import net.minecraft.server.dedicated.command.PardonIpCommand;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class CustomLan implements ModInitializer {
    public static final String MODID = "customlan";

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

    private static final Text SERVER_STOPPED_TEXT = Text.translatable("multiplayer.disconnect.server_shutdown");

    public static String processMotd(MinecraftServer server, String rawMotd) {
        HashMap<String, String> motdMap = new HashMap<>();
        motdMap.put("username", server.getHostProfile().getName());
        motdMap.put("world", server.getSaveProperties().getLevelName());
        StringSubstitutor motdSubstitutor = new StringSubstitutor(motdMap);
        return motdSubstitutor.replace(rawMotd)
                // Replace unescaped ampersands with section signs.
                .replaceAll("((?:[^&]|^)(?:&&)*)&(?!&)", "$1§").replace("&&", "&");
    }

    public static void startOrSaveLan(MinecraftServer server, GameMode gameMode,
            boolean onlineMode, boolean pvpEnabled, int port, int maxPlayers, String rawMotd,
            Consumer<String> onStarted, Consumer<String> onSaved, Runnable onFailed, IntConsumer onPortChangeFailed) {
        PlayerManager playerManager = server.getPlayerManager();

        server.setOnlineMode(onlineMode);
        server.setPvpEnabled(pvpEnabled);

        ((PlayerManagerAccessor) playerManager).setMaxPlayers(maxPlayers);

        String oldMotd = server.getServerMotd();
        String motd = processMotd(server, rawMotd);
        ((HasRawMotd) server).setRawMotd(rawMotd);
        server.setMotd(motd);
        // Metadata doesn't get updated automatically.
        server.forcePlayerSampleUpdate();

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
                    onPortChangeFailed.accept(oldPort);
                }
            }

            if (portChanged || !motd.equals(oldMotd)) {
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

            server.setDefaultGameMode(gameMode);
            // The players' permissions may have changed, so send the new command trees.
            for (ServerPlayerEntity player : playerManager.getPlayerList()) {
                playerManager.sendCommandTree(player); // Do not use server.getCommandManager().sendCommandTree(player)
                                                       // directly or things like the gamemode switcher will not update!
            }
            onSaved.accept(motd);
        } else {
            if (server.openToLan(gameMode, false, port)) {
                server.setDefaultGameMode(gameMode); // Prevents the gamemode from being forced.
                onStarted.accept(motd);
            } else {
                onFailed.run();
            }
            // Update the window title to have " - Multiplayer (LAN)".
            MinecraftClient.getInstance().updateWindowTitle();
        }
    }

    public static void stopLan(MinecraftServer server) {
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
        ((IntegratedServerAccessor) server).getLanPinger().interrupt();

        // Update the window title to bring back " - Singleplayer".
        MinecraftClient.getInstance().updateWindowTitle();
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (environment.integrated) {
                OpCommand.register(dispatcher);
                DeOpCommand.register(dispatcher);
                BanCommand.register(dispatcher);
                BanIpCommand.register(dispatcher);
                BanListCommand.register(dispatcher);
                PardonCommand.register(dispatcher);
                PardonIpCommand.register(dispatcher);
                WhitelistCommand.register(dispatcher);
            }
        });

        ArgumentTypeRegistry.registerArgumentType(new Identifier(MODID, "game_mode"), GameModeArgumentType.class,
                ConstantArgumentSerializer.of(GameModeArgumentType::gameMode));
    }
}
