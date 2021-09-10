package com.dimitrodam.customlan;

import java.util.function.Function;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;

public class PublishCommandArgumentValues {
    public static final int DEFAULT_PORT = 25565;
    public static final boolean DEFAULT_ONLINE_MODE = true;
    public static final boolean DEFAULT_PVP_ENABLED = true;
    public static final int DEFAULT_MAX_PLAYERS = 8;

    public Function<CommandContext<ServerCommandSource>, Integer> getPort = context -> DEFAULT_PORT;
    public Function<CommandContext<ServerCommandSource>, Boolean> getOnlineMode = context -> DEFAULT_ONLINE_MODE;
    public Function<CommandContext<ServerCommandSource>, Boolean> getPvpEnabled = context -> DEFAULT_PVP_ENABLED;
    public Function<CommandContext<ServerCommandSource>, Integer> getMaxPlayers = context -> DEFAULT_MAX_PLAYERS;
    public Function<CommandContext<ServerCommandSource>, String> getMotd = context -> null;

    public PublishCommandArgumentValues() {
    }

    public PublishCommandArgumentValues(PublishCommandArgumentValues other) {
        this.getPort = other.getPort;
        this.getOnlineMode = other.getOnlineMode;
        this.getPvpEnabled = other.getPvpEnabled;
        this.getMaxPlayers = other.getMaxPlayers;
        this.getMotd = other.getMotd;
    }
}