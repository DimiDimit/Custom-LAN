package com.dimitrodam.customlan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameMode;

public class LanSettings {
    public static final boolean DEFAULT_ONLINE_MODE = true;
    public static final boolean DEFAULT_PVP_ENABLED = true;
    public static final TunnelType DEFAULT_TUNNEL = TunnelType.NONE;
    public static final int DEFAULT_PORT = 25565;
    public static final int DEFAULT_MAX_PLAYERS = 8;
    public static final String DEFAULT_MOTD = "${username} - ${world}";

    public static final Codec<LanSettings> CODEC = RecordCodecBuilder
            .create(instance -> instance.group(GameMode.CODEC
                    .fieldOf("gameMode").forGetter(lanSettings -> lanSettings.gameMode),
                    Codec.BOOL.optionalFieldOf("onlineMode", DEFAULT_ONLINE_MODE)
                            .forGetter(lanSettings -> lanSettings.onlineMode),
                    Codec.BOOL.optionalFieldOf("pvpEnabled", DEFAULT_PVP_ENABLED)
                            .forGetter(lanSettings -> lanSettings.pvpEnabled),
                    TunnelType.CODEC.optionalFieldOf("tunnel", DEFAULT_TUNNEL)
                            .forGetter(lanSettings -> lanSettings.tunnel),
                    Codec.INT.optionalFieldOf("port", DEFAULT_PORT).forGetter(lanSettings -> lanSettings.port),
                    Codec.INT.optionalFieldOf("maxPlayers", DEFAULT_MAX_PLAYERS)
                            .forGetter(lanSettings -> lanSettings.maxPlayers),
                    Codec.STRING.optionalFieldOf("motd", DEFAULT_MOTD)
                            .forGetter(lanSettings -> lanSettings.motd))
                    .apply(instance, LanSettings::new));

    public GameMode gameMode;
    public boolean onlineMode;
    public boolean pvpEnabled;
    public TunnelType tunnel;
    public int port;
    public int maxPlayers;
    public String motd;

    public LanSettings(GameMode gameMode, boolean onlineMode, boolean pvpEnabled, TunnelType tunnel, int port,
            int maxPlayers, String motd) {
        this.gameMode = gameMode;
        this.onlineMode = onlineMode;
        this.pvpEnabled = pvpEnabled;
        this.tunnel = tunnel;
        this.port = port;
        this.maxPlayers = maxPlayers;
        this.motd = motd;
    }

    public static LanSettings systemDefaults(MinecraftServer server) {
        return systemDefaults(server.getDefaultGameMode());
    }

    public static LanSettings systemDefaults(GameMode gameMode) {
        return new LanSettings(gameMode, DEFAULT_ONLINE_MODE, DEFAULT_PVP_ENABLED, DEFAULT_TUNNEL, DEFAULT_PORT,
                DEFAULT_MAX_PLAYERS, DEFAULT_MOTD);
    }

    public static LanSettings fromNbt(NbtCompound nbt) {
        LanSettings lanSettings = systemDefaults(GameMode.byIndex(nbt.getInt("gameMode").get()));
        nbt.getBoolean("onlineMode").ifPresent(onlineMode -> lanSettings.onlineMode = onlineMode);
        nbt.getBoolean("pvpEnabled").ifPresent(pvpEnabled -> lanSettings.pvpEnabled = pvpEnabled);
        nbt.getString("tunnel").ifPresent(tunnel -> lanSettings.tunnel = TunnelType.valueOf(tunnel));
        nbt.getInt("port").ifPresent(port -> lanSettings.port = port);
        nbt.getInt("maxPlayers").ifPresent(maxPlayers -> lanSettings.maxPlayers = maxPlayers);
        nbt.getString("motd").ifPresent(motd -> lanSettings.motd = motd);
        return lanSettings;
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("gameMode", gameMode.getIndex());
        nbt.putBoolean("onlineMode", onlineMode);
        nbt.putBoolean("pvpEnabled", pvpEnabled);
        nbt.putString("tunnel", tunnel.name());
        nbt.putInt("port", port);
        nbt.putInt("maxPlayers", maxPlayers);
        nbt.putString("motd", motd);
        return nbt;
    }
}
