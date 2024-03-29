package com.dimitrodam.customlan;

import java.util.function.Function;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameMode;

public class PublishCommandArgumentValues {
    public Function<CommandContext<ServerCommandSource>, Integer> getPort;
    public Function<CommandContext<ServerCommandSource>, Boolean> getOnlineMode;
    public Function<CommandContext<ServerCommandSource>, Boolean> getPvpEnabled;
    public Function<CommandContext<ServerCommandSource>, Integer> getMaxPlayers;
    public Function<CommandContext<ServerCommandSource>, GameMode> getGameMode;
    public Function<CommandContext<ServerCommandSource>, TunnelType> getTunnel;
    public Function<CommandContext<ServerCommandSource>, String> getMotd;

    private static <T> T getValue(Function<CommandContext<ServerCommandSource>, LanSettings> getDefaultLanSettings,
            CommandContext<ServerCommandSource> context, Function<LanSettings, T> getSetting) {
        MinecraftServer server = context.getSource().getServer();

        LanSettings defaultLanSettings = getDefaultLanSettings.apply(context);
        return getSetting.apply(
                defaultLanSettings != null ? defaultLanSettings : LanSettings.systemDefaults(server));
    }

    public PublishCommandArgumentValues(
            Function<CommandContext<ServerCommandSource>, LanSettings> getDefaultLanSettings) {
        this.getPort = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.port);
        this.getOnlineMode = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.onlineMode);
        this.getPvpEnabled = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.pvpEnabled);
        this.getMaxPlayers = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.maxPlayers);
        this.getGameMode = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.gameMode);
        this.getTunnel = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.tunnel);
        this.getMotd = context -> getValue(getDefaultLanSettings, context,
                defaultLanSettings -> defaultLanSettings.motd);
    }

    public PublishCommandArgumentValues(PublishCommandArgumentValues other) {
        this.getPort = other.getPort;
        this.getOnlineMode = other.getOnlineMode;
        this.getPvpEnabled = other.getPvpEnabled;
        this.getMaxPlayers = other.getMaxPlayers;
        this.getGameMode = other.getGameMode;
        this.getTunnel = other.getTunnel;
        this.getMotd = other.getMotd;
    }
}