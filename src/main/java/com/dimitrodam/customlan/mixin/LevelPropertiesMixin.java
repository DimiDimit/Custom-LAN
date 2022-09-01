package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.dimitrodam.customlan.SetCommandsAllowed;

import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin implements SetCommandsAllowed {
    @Shadow
    private LevelInfo levelInfo;

    @Unique
    @Override
    public void setCommandsAllowed(boolean allowCommands) {
        this.levelInfo = new LevelInfo(levelInfo.getLevelName(), levelInfo.getGameMode(), levelInfo.isHardcore(),
                levelInfo.getDifficulty(), allowCommands, levelInfo.getGameRules(), levelInfo.getDataPackSettings());
    }
}
