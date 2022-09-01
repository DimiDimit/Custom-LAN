package com.dimitrodam.customlan;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameMode;

public class LanSettings {
    public GameMode gameMode;
    public boolean onlineMode;
    public boolean pvpEnabled;
    public int port;
    public int maxPlayers;
    public String motd;

    public LanSettings(GameMode gameMode, boolean onlineMode, boolean pvpEnabled, int port, int maxPlayers,
            String motd) {
        this.gameMode = gameMode;
        this.onlineMode = onlineMode;
        this.pvpEnabled = pvpEnabled;
        this.port = port;
        this.maxPlayers = maxPlayers;
        this.motd = motd;
    }

    public static LanSettings systemDefaults(MinecraftServer server) {
        return new LanSettings(server.getDefaultGameMode(), true, true, 25565, 8, "${username} - ${world}");
    }

    public static LanSettings fromNbt(NbtCompound nbt) {
        return new LanSettings(GameMode.byId(nbt.getInt("gameMode")), nbt.getBoolean("onlineMode"),
                nbt.getBoolean("pvpEnabled"), nbt.getInt("port"), nbt.getInt("maxPlayers"), nbt.getString("motd"));
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("gameMode", gameMode.getId());
        nbt.putBoolean("onlineMode", onlineMode);
        nbt.putBoolean("pvpEnabled", pvpEnabled);
        nbt.putInt("port", port);
        nbt.putInt("maxPlayers", maxPlayers);
        nbt.putString("motd", motd);
        return nbt;
    }
}
