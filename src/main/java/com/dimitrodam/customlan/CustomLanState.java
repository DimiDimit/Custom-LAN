package com.dimitrodam.customlan;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class CustomLanState extends PersistentState {
    public static final String CUSTOM_LAN_KEY = CustomLan.MODID;
    private static final String LAN_SETTINGS_KEY = "lanSettings";
    private static final String WHITELIST_ENABLED_KEY = "whitelistEnabled";

    @Nullable
    private LanSettings lanSettings;
    private boolean whitelistEnabled;

    private CustomLanState(@Nullable LanSettings lanSettings, boolean whitelistEnabled) {
        this.lanSettings = lanSettings;
        this.whitelistEnabled = whitelistEnabled;
    }

    public CustomLanState() {
        this.lanSettings = null;
        this.whitelistEnabled = false;
    }

    public static CustomLanState fromNbt(NbtCompound nbt) {
        NbtCompound lanSettingsNbt = nbt.getCompound(LAN_SETTINGS_KEY);
        return new CustomLanState(lanSettingsNbt.isEmpty() ? null : LanSettings.fromNbt(lanSettingsNbt),
                nbt.getBoolean(WHITELIST_ENABLED_KEY));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (lanSettings != null) {
            NbtCompound lanSettingsNbt = new NbtCompound();
            lanSettings.writeNbt(lanSettingsNbt);
            nbt.put(LAN_SETTINGS_KEY, lanSettingsNbt);
        }
        nbt.putBoolean(WHITELIST_ENABLED_KEY, this.whitelistEnabled);
        return nbt;
    }

    @Nullable
    public LanSettings getLanSettings() {
        return this.lanSettings;
    }

    public boolean getWhitelistEnabled() {
        return this.whitelistEnabled;
    }

    public void setLanSettings(@Nullable LanSettings lanSettings) {
        this.lanSettings = lanSettings;
        this.markDirty();
    }

    public void setWhitelistEnabled(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
        this.markDirty();
    }
}
