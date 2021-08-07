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
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final String FIND_LOCAL_PORT = "net/minecraft/client/util/NetworkUtils.findLocalPort()I";

    private static final int DEFAULT_PORT = 25565;
    private static final String ONLINE_MODE_TEXT = "lanServer.onlineMode";
    private static final String PVP_ENABLED_TEXT = "lanServer.pvpEnabled";
    private static final String PORT_TEXT = "lanServer.port";
    private static final String MOTD_TEXT = "lanServer.motd";

    private ButtonWidget onlineModeButton;
    private ButtonWidget pvpEnabledButton;
    private TextFieldWidget portField;
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

    @Shadow
    private void updateButtonText() {
        throw new AssertionError();
    }

    // Custom buttons must be created before the call to updateButtonText
    // at the end of the Vanilla method. Hence, @At("HEAD").
    @Inject(method = "init", at = @At("HEAD"))
    private void addCustomWidgets(CallbackInfo ci) {
        // Online Mode button
        this.onlineModeButton = this.addButton(
                new ButtonWidget(this.width / 2 - 155, 124, 150, 20, I18n.translate(ONLINE_MODE_TEXT), (button) -> {
                    this.onlineMode = !this.onlineMode;
                    this.updateButtonText();
                }));
        // PvP Enabled button
        this.pvpEnabledButton = this.addButton(
                new ButtonWidget(this.width / 2 + 5, 124, 150, 20, I18n.translate(PVP_ENABLED_TEXT), (button) -> {
                    this.pvpEnabled = !this.pvpEnabled;
                    this.updateButtonText();
                }));

        // Port field
        this.portField = new TextFieldWidget(this.font, this.width / 2 - 154, this.height - 54, 148, 20,
                I18n.translate(PORT_TEXT));
        portField.setText(Integer.toString(port));
        portField.setChangedListener((port) -> {
            ButtonWidget startButton = (ButtonWidget) this.children().get(4);
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
        this.children.add(portField);

        // MOTD field
        IntegratedServer server = this.minecraft.getServer();
        this.motdField = new TextFieldWidget(this.font, this.width / 2 + 6, this.height - 54, 148, 20,
                I18n.translate(MOTD_TEXT));
        motdField.setText(server.getServerMotd());
        this.children.add(motdField);
    }

    @Inject(method = "updateButtonText", at = @At("TAIL"))
    private void updateCustomButtonText(CallbackInfo ci) {
        this.onlineModeButton.setMessage(I18n.translate(ONLINE_MODE_TEXT) + ": "
                + I18n.translate(this.onlineMode ? "options.on" : "options.off"));
        this.pvpEnabledButton.setMessage(I18n.translate(PVP_ENABLED_TEXT) + ": "
                + I18n.translate(this.pvpEnabled ? "options.on" : "options.off"));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustom(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Port field text
        drawString(this.font, I18n.translate(PORT_TEXT), this.width / 2 - 154, this.height - 66, 10526880);
        // MOTD field text
        drawString(this.font, I18n.translate(MOTD_TEXT), this.width / 2 + 6, this.height - 66, 10526880);

        this.portField.render(mouseX, mouseY, delta);
        this.motdField.render(mouseX, mouseY, delta);
    }

    @Inject(method = "method_19851", at = @At("HEAD"))
    private void beforeOpenedToLan(CallbackInfo ci) {
        IntegratedServer server = this.minecraft.getServer();

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
     * Allow starting with enter as well as the Open to LAN button.
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
                this.method_19851((ButtonWidget) this.children().get(4));
            }
            return true;
        }
    }
}
