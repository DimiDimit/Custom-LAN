package com.dimitrodam.customlan.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final String FIND_LOCAL_PORT = "net/minecraft/client/util/NetworkUtils.findLocalPort()I";

    private static final int DEFAULT_PORT = 25565;
    private static final Text ONLINE_MODE_TEXT = new TranslatableText("lanServer.onlineMode");
    private static final Text PVP_ENABLED_TEXT = new TranslatableText("lanServer.pvpEnabled");
    private static final Text PORT_TEXT = new TranslatableText("lanServer.port");
    private static final Text MOTD_TEXT = new TranslatableText("lanServer.motd");

    private TextFieldWidget motdField;

    private boolean onlineMode = true;
    private boolean pvpEnabled = true;
    private int port = DEFAULT_PORT;
    private boolean portValid = true;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    private void method_19851(ButtonWidget button) {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCustomWidgets(CallbackInfo ci) {
        // Online Mode button
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(onlineMode).build(this.width / 2 - 155, 124, 150, 20,
                ONLINE_MODE_TEXT, (button, onlineMode) -> {
                    this.onlineMode = onlineMode;
                }));
        // PvP Enabled button
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(pvpEnabled).build(this.width / 2 + 5, 124, 150, 20,
                PVP_ENABLED_TEXT, (button, pvpEnabled) -> {
                    this.pvpEnabled = pvpEnabled;
                }));

        // Port field
        ButtonWidget startButton = (ButtonWidget) this.children().get(2);
        TextFieldWidget portField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 54, 148,
                20, PORT_TEXT);
        portField.setText(Integer.toString(port));
        portField.setChangedListener((port) -> {
            Integer newPort = null;
            try {
                newPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
            if (newPort != null && newPort > 0 && newPort <= 65535) {
                this.port = newPort;
                this.portValid = true;
                portField.setEditableColor(0xFFFFFF);
                startButton.active = true;
            } else {
                this.port = DEFAULT_PORT;
                this.portValid = false;
                portField.setEditableColor(0xFF0000);
                startButton.active = false;
            }
        });
        this.addDrawableChild(portField);

        // MOTD field
        IntegratedServer server = this.client.getServer();
        this.motdField = new TextFieldWidget(this.textRenderer, this.width / 2 + 6, this.height - 54, 148, 20,
                MOTD_TEXT);
        motdField.setMaxLength(59); // https://minecraft.fandom.com/wiki/Server.properties#motd
        motdField.setText(server.getServerMotd());
        this.addDrawableChild(motdField);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Port field text
        drawTextWithShadow(matrices, this.textRenderer, PORT_TEXT, this.width / 2 - 154, this.height - 66, 10526880);
        // MOTD field text
        drawTextWithShadow(matrices, this.textRenderer, MOTD_TEXT, this.width / 2 + 6, this.height - 66, 10526880);
    }

    @Inject(method = "method_19851", at = @At("HEAD"))
    private void beforeOpenedToLan(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        server.setOnlineMode(this.onlineMode);
        server.setPvpEnabled(this.pvpEnabled);

        // MOTD needs to be set before openToLan
        // because the LAN pinger calls getServerMotd.
        server.setMotd(this.motdField.getText());
        // Metadata doesn't get updated automatically.
        server.getServerMetadata().setDescription(new LiteralText(server.getServerMotd()));
    }

    @Redirect(method = "method_19851", at = @At(value = "INVOKE", ordinal = 0, target = FIND_LOCAL_PORT))
    private int getPort() {
        return this.port;
    }

    /**
     * Allow starting with Enter as well as the Start LAN World button.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        } else {
            if (this.portValid) {
                // Open to LAN
                this.method_19851((ButtonWidget) this.children().get(2));
            }
            return true;
        }
    }
}
