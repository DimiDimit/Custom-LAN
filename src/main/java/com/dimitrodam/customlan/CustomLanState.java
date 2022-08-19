package com.dimitrodam.customlan;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class CustomLanState extends PersistentState {
    public static final String CUSTOM_LAN_KEY = CustomLan.MODID;
    private static final String WHITELIST_ENABLED_KEY = "whitelistEnabled";

    private boolean whitelistEnabled;

    private CustomLanState(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
    }

    public CustomLanState() {
        this.whitelistEnabled = false;
    }

    public static CustomLanState fromNbt(NbtCompound nbt) {
        return new CustomLanState(nbt.getBoolean(WHITELIST_ENABLED_KEY));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean(WHITELIST_ENABLED_KEY, this.whitelistEnabled);
        return nbt;
    }

    public boolean getWhitelistEnabled() {
        return this.whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean whitelistEnabled) {
        this.whitelistEnabled = whitelistEnabled;
        this.markDirty();
    }
}
