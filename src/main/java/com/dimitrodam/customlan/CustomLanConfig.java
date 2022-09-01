package com.dimitrodam.customlan;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.NoSuchFileException;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;

public class CustomLanConfig {
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(),
            CustomLan.MODID + ".json");

    public static CustomLanConfig INSTANCE = reload();

    @Nullable
    private LanSettings lanSettings;

    private CustomLanConfig(@Nullable LanSettings lanSettings) {
        this.lanSettings = lanSettings;
    }

    public static CustomLanConfig reload() {
        try (FileReader reader = new FileReader(FILE)) {
            return INSTANCE = CustomLan.GSON.fromJson(reader, CustomLanConfig.class);
        } catch (NoSuchFileException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return INSTANCE = new CustomLanConfig(null);
    }

    public void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            CustomLan.GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public LanSettings getLanSettings() {
        return this.lanSettings;
    }

    public void setLanSettings(@Nullable LanSettings lanSettings) {
        this.lanSettings = lanSettings;
        this.save();
    }
}
