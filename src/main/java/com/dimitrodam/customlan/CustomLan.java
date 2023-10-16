package com.dimitrodam.customlan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.Nullable;

import com.dimitrodam.customlan.TunnelType.TunnelException;
import com.dimitrodam.customlan.command.argument.GameModeArgumentType;
import com.dimitrodam.customlan.command.argument.TunnelArgumentType;
import com.dimitrodam.customlan.mixin.IntegratedServerAccessor;
import com.dimitrodam.customlan.mixin.PlayerManagerAccessor;
import com.github.alexdlaird.ngrok.NgrokClient;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
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
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

public class CustomLan implements ModInitializer {
    public static final String MODID = "customlan";

    public static ConfigHolder<CustomLanConfig> CONFIG;

    @Nullable
    public static NgrokClient NGROK_CLIENT;

    private static final String PUBLISH_STARTED_CUSTOMLAN_TEXT = "commands.publish.started.customlan";
    private static final String PUBLISH_STARTED_CUSTOMLAN_TUNNEL_TEXT = "commands.publish.started.customlan.tunnel";
    private static final String PUBLISH_PORT_CHANGE_FAILED_TEXT = "commands.publish.failed.port_change";
    private static final String PUBLISH_SAVED_TEXT = "commands.publish.saved";
    private static final String PUBLISH_SAVED_TUNNEL_TEXT = "commands.publish.saved.tunnel";
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

    public static void startOrSaveLan(MinecraftServer server, GameMode gameMode, boolean onlineMode, boolean pvpEnabled,
            TunnelType tunnel, int port, int maxPlayers, String rawMotd, Consumer<Text> onSuccess,
            Runnable onFatalError, Consumer<Text> onNonFatalError) {
        CustomLanServerValues serverValues = (CustomLanServerValues) server;
        PlayerManager playerManager = server.getPlayerManager();

        server.setOnlineMode(onlineMode);
        server.setPvpEnabled(pvpEnabled);

        ((PlayerManagerAccessor) playerManager).setMaxPlayers(maxPlayers);

        String oldMotd = server.getServerMotd();
        String motd = processMotd(server, rawMotd);
        serverValues.setRawMotd(rawMotd);
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
                    onNonFatalError.accept(Text.translatable(PUBLISH_PORT_CHANGE_FAILED_TEXT,
                            Texts.bracketedCopyable(String.valueOf(oldPort))));
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

            TunnelType oldTunnel = serverValues.getTunnelType();
            if (tunnel != oldTunnel || portChanged) {
                try {
                    oldTunnel.stop(server);
                } catch (TunnelException e) {
                    onNonFatalError.accept(e.getMessageText());
                }

                try {
                    Text tunnelText = tunnel.start(server);
                    serverValues.setTunnelType(tunnel);
                    serverValues.setTunnelText(tunnelText);
                    if (tunnelText != null) {
                        onSuccess.accept(Text.translatable(PUBLISH_SAVED_TUNNEL_TEXT,
                                Texts.bracketedCopyable(String.valueOf(server.getServerPort())), tunnelText, motd));
                    } else {
                        onSuccess.accept(Text.translatable(PUBLISH_SAVED_TEXT,
                                Texts.bracketedCopyable(String.valueOf(server.getServerPort())), motd));
                    }
                } catch (TunnelException e) {
                    onSuccess.accept(Text.translatable(PUBLISH_SAVED_TEXT,
                            Texts.bracketedCopyable(String.valueOf(server.getServerPort())), motd));
                    onNonFatalError.accept(e.getMessageText());
                }
            } else {
                if (serverValues.getTunnelText() != null) {
                    onSuccess.accept(Text.translatable(PUBLISH_SAVED_TUNNEL_TEXT,
                            Texts.bracketedCopyable(String.valueOf(server.getServerPort())),
                            serverValues.getTunnelText(), motd));
                } else {
                    onSuccess.accept(Text.translatable(PUBLISH_SAVED_TEXT,
                            Texts.bracketedCopyable(String.valueOf(server.getServerPort())), motd));
                }
            }
        } else {
            if (server.openToLan(gameMode, false, port)) {
                server.setDefaultGameMode(gameMode); // Prevents the gamemode from being forced.

                try {
                    Text tunnelText = tunnel.start(server);
                    serverValues.setTunnelType(tunnel);
                    serverValues.setTunnelText(tunnelText);
                    if (tunnelText != null) {
                        onSuccess.accept(Text.translatable(PUBLISH_STARTED_CUSTOMLAN_TUNNEL_TEXT,
                                Texts.bracketedCopyable(String.valueOf(server.getServerPort())), tunnelText, motd));
                    } else {
                        onSuccess.accept(Text.translatable(PUBLISH_STARTED_CUSTOMLAN_TEXT,
                                Texts.bracketedCopyable(String.valueOf(server.getServerPort())), motd));
                    }
                } catch (TunnelException e) {
                    onSuccess.accept(Text.translatable(PUBLISH_STARTED_CUSTOMLAN_TEXT,
                            Texts.bracketedCopyable(String.valueOf(server.getServerPort())), motd));
                    onNonFatalError.accept(e.getMessageText());
                }
            } else {
                onFatalError.run();
            }
            // Update the window title to have " - Multiplayer (LAN)".
            MinecraftClient.getInstance().updateWindowTitle();
        }
    }

    public static void stopLan(MinecraftServer server, Runnable onSuccess,
            Runnable onFatalError, Consumer<Text> onNonFatalError) {
        CustomLanServerValues serverValues = (CustomLanServerValues) server;

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

        onSuccess.run();

        try {
            serverValues.getTunnelType().stop(server);
            serverValues.setTunnelType(TunnelType.NONE);
            serverValues.setTunnelText(null);
        } catch (TunnelException e) {
            onNonFatalError.accept(e.getMessageText());
        }

        // Update the window title to bring back " - Singleplayer".
        MinecraftClient.getInstance().updateWindowTitle();
    }

    @Override
    public void onInitialize() {
        Thread ngrokInstallThread = new Thread(() -> new NgrokClient.Builder().build());
        ngrokInstallThread.start();

        AutoConfig.register(CustomLanConfig.class, Toml4jConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(CustomLanConfig.class);

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
        ArgumentTypeRegistry.registerArgumentType(new Identifier(MODID, "tunnel"), TunnelArgumentType.class,
                ConstantArgumentSerializer.of(TunnelArgumentType::tunnel));
    }
}
