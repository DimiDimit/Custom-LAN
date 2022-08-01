package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
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

        boolean isHost = this.client.isIntegratedServerRunning();
        if (isHost && server.isRemote()) { // Already opened to LAN
            ButtonWidget playerReportingButton = (ButtonWidget) this.children().get(6);
            playerReportingButton.setMessage(EDIT_LAN_TEXT);
            ((ButtonWidgetAccessor) playerReportingButton)
                    .setOnPress(button -> this.client.setScreen(new OpenToLanScreen(this)));
        }
    }
}
