package com.dimitrodam.customlan;

import java.util.function.Function;

import com.dimitrodam.customlan.util.ArgumentValueFunction;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameMode;

public class PublishCommandArgumentValues {
    public ArgumentValueFunction<Integer> getPort;
    public ArgumentValueFunction<Boolean> getOnlineMode;
    public ArgumentValueFunction<Boolean> getPvpEnabled;
    public ArgumentValueFunction<Integer> getMaxPlayers;
    public ArgumentValueFunction<GameMode> getGameMode;
    public ArgumentValueFunction<TunnelType> getTunnel;
    public ArgumentValueFunction<String> getMotd;

    private static <T> T getValue(ArgumentValueFunction<LanSettings> getDefaultLanSettings,
            CommandContext<ServerCommandSource> context, Function<LanSettings, T> getSetting)
            throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();

        LanSettings defaultLanSettings = getDefaultLanSettings.apply(context);
        return getSetting.apply(
                defaultLanSettings != null ? defaultLanSettings : LanSettings.systemDefaults(server));
    }

    public PublishCommandArgumentValues(
            ArgumentValueFunction<LanSettings> getDefaultLanSettings) {
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