package com.dimitrodam.customlan.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    private static final Text PLAYER_REPORTING_TEXT = Text.translatable("menu.playerReporting");

    private static final Text EDIT_LAN_TEXT = Text.translatable("menu.editLan");

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void updateOpenToLanButton(CallbackInfo ci, GridWidget gridWidget) {
        IntegratedServer server = this.client.getServer();

        boolean isHost = this.client.isIntegratedServerRunning();
        if (isHost && server.isRemote()) { // Already opened to LAN
            for (Widget widget : ((GridWidgetAccessor) gridWidget).getChildren()) {
                if (!(widget instanceof ButtonWidget)) {
                    continue;
                }
                ButtonWidget button = (ButtonWidget) widget;
                if (PLAYER_REPORTING_TEXT.equals(button.getMessage())) {
                    button.setMessage(EDIT_LAN_TEXT);
                    ((ButtonWidgetAccessor) button)
                            .setOnPress(btn -> this.client.setScreen(new OpenToLanScreen(this)));
                }
            }
        }
    }
}
