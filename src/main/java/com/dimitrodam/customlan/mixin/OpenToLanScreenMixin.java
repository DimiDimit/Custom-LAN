package com.dimitrodam.customlan.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.LanServerPinger;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final int DEFAULT_PORT = 25565;
    private static final Text SERVER_STOPPED_TEXT = new TranslatableText("multiplayer.disconnect.server_shutdown");
    private static final String PUBLISH_STARTED_TEXT = "commands.publish.started";
    private static final Text PUBLISH_FAILED_TEXT = new TranslatableText("commands.publish.failed");
    private static final String PUBLISH_PORT_CHANGE_FAILED_TEXT = "commands.publish.failed.port_change";
    private static final Text PUBLISH_SAVED_TEXT = new TranslatableText("commands.publish.saved");
    private static final Text PUBLISH_STOPPED_TEXT = new TranslatableText("commands.publish.stopped");
    private static final Text ALLOW_COMMANDS_EXPLANATION_TEXT = new TranslatableText(
            "lanServer.allowCommandsExplanation");
    private static final Text START_TEXT = new TranslatableText("lanServer.start");
    private static final Text SAVE_TEXT = new TranslatableText("lanServer.save");
    private static final Text STOP_TEXT = new TranslatableText("lanServer.stop");
    private static final Text ONLINE_MODE_TEXT = new TranslatableText("lanServer.onlineMode");
    private static final Text PVP_ENABLED_TEXT = new TranslatableText("lanServer.pvpEnabled");
    private static final Text PORT_TEXT = new TranslatableText("lanServer.port");
    private static final Text MAX_PLAYERS_TEXT = new TranslatableText("lanServer.maxPlayers");
    private static final Text MOTD_TEXT = new TranslatableText("lanServer.motd");

    private MultilineText allowCommandsExplanationText = MultilineText.EMPTY;
    private ButtonWidget onlineModeButton;
    private ButtonWidget pvpEnabledButton;
    private TextFieldWidget motdField;
    private ButtonWidget startSaveButton;

    private boolean onlineMode;
    private boolean pvpEnabled;
    private int port;
    private boolean portValid = true;
    private int maxPlayers;
    private boolean maxPlayersValid = true;

    @Shadow
    private ButtonWidget buttonGameMode;
    @Shadow
    private ButtonWidget buttonAllowCommands;

    @Shadow
    private String gameMode;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    private void updateButtonText() {
        throw new AssertionError();
    }

    private boolean canStartOrSave() {
        return this.portValid && this.maxPlayersValid;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void setDefaults(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        this.gameMode = server.getDefaultGameMode().getName();
        this.onlineMode = server.isOnlineMode();
        this.pvpEnabled = server.isPvpEnabled();
        this.port = server.isRemote() ? server.getServerPort() : DEFAULT_PORT;
        this.maxPlayers = server.getMaxPlayerCount();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCustomWidgets(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();
        boolean alreadyOpenedToLan = server.isRemote();

        // Replace the Allow Cheats button
        // with explanation text below it (added in renderText)
        // and have the Game Mode button fill its place.
        this.buttons.remove(this.buttonAllowCommands);
        this.children.remove(this.buttonAllowCommands);
        this.buttonGameMode.setWidth(310);
        this.allowCommandsExplanationText = MultilineText.create(this.textRenderer, ALLOW_COMMANDS_EXPLANATION_TEXT,
                308);

        // Online Mode button
        this.onlineModeButton = this
                .addButton(new ButtonWidget(this.width / 2 - 155, 124, 150, 20, ONLINE_MODE_TEXT, button -> {
                    this.onlineMode = !this.onlineMode;
                    this.updateButtonText();
                }));
        // PvP Enabled button
        this.pvpEnabledButton = this
                .addButton(new ButtonWidget(this.width / 2 + 5, 124, 150, 20, PVP_ENABLED_TEXT, button -> {
                    this.pvpEnabled = !this.pvpEnabled;
                    this.updateButtonText();
                }));

        // Port field
        TextFieldWidget portField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 92, 148,
                20, PORT_TEXT);
        portField.setText(Integer.toString(port));
        portField.setChangedListener(port -> {
            Integer newPort = null;
            try {
                newPort = Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
            if (newPort != null && newPort > 0 && newPort <= 65535) {
                this.port = newPort;
                this.portValid = true;
                portField.setEditableColor(0xFFFFFF);
            } else {
                this.portValid = false;
                portField.setEditableColor(0xFF0000);
            }
            this.startSaveButton.active = this.canStartOrSave();
        });
        this.addButton(portField);

        // Max Players field
        TextFieldWidget maxPlayersField = new TextFieldWidget(this.textRenderer, this.width / 2 + 6, this.height - 92,
                148, 20, MAX_PLAYERS_TEXT);
        maxPlayersField.setText(Integer.toString(maxPlayers));
        maxPlayersField.setChangedListener(maxPlayers -> {
            Integer newMaxPlayers = null;
            try {
                newMaxPlayers = Integer.parseInt(maxPlayers);
            } catch (NumberFormatException e) {
            }
            if (newMaxPlayers != null && newMaxPlayers > 0) {
                this.maxPlayers = newMaxPlayers;
                this.maxPlayersValid = true;
                maxPlayersField.setEditableColor(0xFFFFFF);
            } else {
                this.maxPlayersValid = false;
                maxPlayersField.setEditableColor(0xFF0000);
            }
            this.startSaveButton.active = this.canStartOrSave();
        });
        this.addButton(maxPlayersField);

        // MOTD field
        this.motdField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 54, 308, 20,
                MOTD_TEXT);
        motdField.setMaxLength(59); // https://minecraft.fandom.com/wiki/Server.properties#motd
        motdField.setText(server.getServerMotd());
        this.addButton(motdField);

        // Replace the Start LAN World button with a Start/Save button.
        this.buttons.remove(0);
        this.children.remove(0);
        this.startSaveButton = this
                .addButton(new ButtonWidget(this.width / 2 - 155, this.height - 28, alreadyOpenedToLan ? 73 : 150, 20,
                        alreadyOpenedToLan ? SAVE_TEXT : START_TEXT, button -> this.startOrSave()));

        if (alreadyOpenedToLan) {
            this.addButton(
                    new ButtonWidget(this.width / 2 - 78, this.height - 28, 73, 20, STOP_TEXT, button -> this.stop()));
        }

        // Move the Cancel button to the end for consistent Tab order.
        ButtonWidget cancelButton = (ButtonWidget) this.children.get(0);
        this.buttons.remove(cancelButton);
        this.children.remove(cancelButton);
        this.addButton(cancelButton);

        this.updateButtonText();
    }

    @Inject(method = "updateButtonText", at = @At("TAIL"))
    private void updateCustomButtonText(CallbackInfo ci) {
        if (this.onlineModeButton != null) {
            this.onlineModeButton.setMessage(ScreenTexts.composeToggleText(ONLINE_MODE_TEXT, this.onlineMode));
        }
        if (this.pvpEnabledButton != null) {
            this.pvpEnabledButton.setMessage(ScreenTexts.composeToggleText(PVP_ENABLED_TEXT, this.pvpEnabled));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Explanation text on how to enable/disable commands
        this.allowCommandsExplanationText.drawWithShadow(matrices, this.width / 2 - 154, 148, 9, 10526880);

        // Port field text
        drawTextWithShadow(matrices, this.textRenderer, PORT_TEXT, this.width / 2 - 154, this.height - 104, 10526880);
        // Max Players field text
        drawTextWithShadow(matrices, this.textRenderer, MAX_PLAYERS_TEXT, this.width / 2 + 6, this.height - 104,
                10526880);
        // MOTD field text
        drawTextWithShadow(matrices, this.textRenderer, MOTD_TEXT, this.width / 2 - 154, this.height - 66, 10526880);
    }

    private void startOrSave() {
        this.client.openScreen(null);

        IntegratedServer server = this.client.getServer();
        PlayerManager playerManager = server.getPlayerManager();

        GameMode gameMode = GameMode.byName(this.gameMode);

        server.setOnlineMode(this.onlineMode);
        server.setPvpEnabled(this.pvpEnabled);

        ((PlayerManagerAccessor) playerManager).setMaxPlayers(this.maxPlayers);

        String oldMotd = server.getServerMotd();
        String motd = this.motdField.getText();
        server.setMotd(motd);
        // Metadata doesn't get updated automatically.
        server.getServerMetadata().setDescription(new LiteralText(motd));

        if (server.isRemote()) { // Already opened to LAN
            int oldPort = server.getServerPort();
            boolean portChanged = false;
            if (this.port != oldPort) {
                ServerNetworkIo networkIo = server.getNetworkIo();
                try {
                    networkIo.bind(null, this.port); // Checks that the port works.
                    networkIo.stop(); // Stops listening on the port, but does not close any existing connections.
                    networkIo.bind(null, this.port); // Actually starts listening on the new port.
                    ((IntegratedServerAccessor) server).setLanPort(this.port);
                    portChanged = true;
                } catch (IOException e) {
                    this.client.inGameHud.getChatHud().addMessage(
                            new TranslatableText(PUBLISH_PORT_CHANGE_FAILED_TEXT, new Object[] { oldPort }));
                }
            }

            if (portChanged || !motd.equals(oldMotd)) {
                // Restart the LAN pinger as its properties are immutable.
                ((IntegratedServerAccessor) server).getLanPinger().interrupt();
                try {
                    LanServerPinger lanPinger = new LanServerPinger(motd, Integer.toString(server.getServerPort()));
                    ((IntegratedServerAccessor) server).setLanPinger(lanPinger);
                    lanPinger.start();
                } catch (IOException e) {
                    // The LAN pinger not working isn't the end of the world.
                }
            }

            server.setDefaultGameMode(gameMode);
            // The players' permissions may have changed, so send the new command trees.
            for (ServerPlayerEntity player : playerManager.getPlayerList()) {
                playerManager.sendCommandTree(player); // Do not use server.getCommandManager().sendCommandTree(player)
                                                       // directly or things like the gamemode switcher will not update!
            }
            this.client.inGameHud.getChatHud().addMessage(PUBLISH_SAVED_TEXT);
        } else {
            Text message;
            if (server.openToLan(gameMode, false, this.port)) {
                server.setDefaultGameMode(gameMode); // Prevents the gamemode from being forced.
                message = new TranslatableText(PUBLISH_STARTED_TEXT, new Object[] { this.port });
            } else {
                message = PUBLISH_FAILED_TEXT;
            }
            this.client.inGameHud.getChatHud().addMessage(message);
            this.client.updateWindowTitle(); // Updates the window title to have " - Multiplayer (LAN)".
        }
    }

    private void stop() {
        this.client.openScreen(null);

        IntegratedServer server = this.client.getServer();

        // Disconnect the connected players.
        UUID localPlayerUuid = ((IntegratedServerAccessor) server).getLocalPlayerUuid();
        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> playerList = new ArrayList<>(playerManager.getPlayerList()); // Needs to be cloned!
        for (ServerPlayerEntity player : playerList) {
            if (!player.getUuid().equals(localPlayerUuid)) {
                player.networkHandler.disconnect(SERVER_STOPPED_TEXT);
            }
        }

        server.getNetworkIo().stop(); // Stops listening on the port, but does not close any existing connections.
        ((IntegratedServerAccessor) server).setLanPort(-1);
        ((IntegratedServerAccessor) server).getLanPinger().interrupt();

        this.client.inGameHud.getChatHud().addMessage(PUBLISH_STOPPED_TEXT);
        this.client.updateWindowTitle(); // Updates the window title to bring back " - Singleplayer".
    }

    /**
     * Allow starting/saving with Enter as well as the Start/Save button.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        } else {
            if (this.canStartOrSave()) {
                this.startOrSave();
            }
            return true;
        }
    }
}
