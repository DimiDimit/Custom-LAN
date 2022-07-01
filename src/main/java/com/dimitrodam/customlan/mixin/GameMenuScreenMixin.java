package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    private static final Text EDIT_LAN_TEXT = Text.translatable("menu.editLan");

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("RETURN"))
    private void updateOpenToLanButton(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        ButtonWidget openToLanButton = (ButtonWidget) this.children().get(6);
        boolean isHost = this.client.isIntegratedServerRunning();
        openToLanButton.active = isHost;
        if (isHost && server.isRemote()) { // Already opened to LAN
            openToLanButton.setMessage(EDIT_LAN_TEXT);
        }
    }
}
