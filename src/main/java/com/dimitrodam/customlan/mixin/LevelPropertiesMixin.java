package com.dimitrodam.customlan.mixin;

import com.dimitrodam.customlan.SetCommandsAllowed;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;

@Mixin(LevelProperties.class)
public class LevelPropertiesMixin implements SetCommandsAllowed {
    @Shadow
    private LevelInfo field_25030; // levelInfo

    @Unique
    @Override
    public void customlan$setCommandsAllowed(boolean allowCommands) {
        this.field_25030 = new LevelInfo(field_25030.getLevelName(), field_25030.getGameMode(),
                field_25030.isHardcore(), field_25030.getDifficulty(), allowCommands, field_25030.getGameRules(),
                field_25030.method_29558());
    }
}
