package com.dimitrodam.customlan;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

public class CustomLanState extends PersistentState {
    private static final String LAN_SETTINGS_KEY = "lanSettings";
    private static final String WHITELIST_ENABLED_KEY = "whitelistEnabled";

    public static final String CUSTOM_LAN_KEY = CustomLan.MODID;
    public static final Codec<CustomLanState> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(LanSettings.CODEC.optionalFieldOf(LAN_SETTINGS_KEY, null).forGetter(state -> state.lanSettings),
                    Codec.BOOL.optionalFieldOf(WHITELIST_ENABLED_KEY, false)
                            .forGetter(state -> state.whitelistEnabled))
            .apply(instance, CustomLanState::new));
    public static final PersistentStateType<CustomLanState> STATE_TYPE = new PersistentStateType<>(CUSTOM_LAN_KEY,
            CustomLanState::new, CODEC, null);

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
