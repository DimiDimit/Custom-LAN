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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.LanServerPinger;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final int DEFAULT_PORT = 25565;
    private static final Text SERVER_STOPPED_TEXT = Text.translatable("multiplayer.disconnect.server_shutdown");
    private static final String PUBLISH_STARTED_TEXT = "commands.publish.started";
    private static final Text PUBLISH_FAILED_TEXT = Text.translatable("commands.publish.failed");
    private static final String PUBLISH_PORT_CHANGE_FAILED_TEXT = "commands.publish.failed.port_change";
    private static final Text PUBLISH_SAVED_TEXT = Text.translatable("commands.publish.saved");
    private static final Text PUBLISH_STOPPED_TEXT = Text.translatable("commands.publish.stopped");
    private static final Text ALLOW_COMMANDS_EXPLANATION_TEXT = Text.translatable(
            "lanServer.allowCommandsExplanation");
    private static final Text START_TEXT = Text.translatable("lanServer.start");
    private static final Text SAVE_TEXT = Text.translatable("lanServer.save");
    private static final Text STOP_TEXT = Text.translatable("lanServer.stop");
    private static final Text ONLINE_MODE_TEXT = Text.translatable("lanServer.onlineMode");
    private static final Text PVP_ENABLED_TEXT = Text.translatable("lanServer.pvpEnabled");
    private static final Text PORT_TEXT = Text.translatable("lanServer.port");
    private static final Text MAX_PLAYERS_TEXT = Text.translatable("lanServer.maxPlayers");
    private static final Text MOTD_TEXT = Text.translatable("lanServer.motd");

    private MultilineText allowCommandsExplanationText = MultilineText.EMPTY;
    private TextFieldWidget motdField;
    private ButtonWidget startSaveButton;

    private boolean onlineMode;
    private boolean pvpEnabled;
    private int port;
    private boolean portValid = true;
    private int maxPlayers;
    private boolean maxPlayersValid = true;

    @Shadow
    private GameMode gameMode;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    private boolean canStartOrSave() {
        return this.portValid && this.maxPlayersValid;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void setDefaults(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        this.gameMode = server.getDefaultGameMode();
        this.onlineMode = server.isOnlineMode();
        this.pvpEnabled = server.isPvpEnabled();
        this.port = server.isRemote() ? server.getServerPort() : DEFAULT_PORT;
        this.maxPlayers = server.getMaxPlayerCount();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "init", at = @At("TAIL"))
    private void addCustomWidgets(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();
        boolean alreadyOpenedToLan = server.isRemote();

        // Replace the Allow Cheats button
        // with explanation text below it (added in renderText)
        // and have the Game Mode button fill its place.
        this.remove(this.children().get(1));
        CyclingButtonWidget<GameMode> gameModeButton = (CyclingButtonWidget<GameMode>) this.children().get(0);
        gameModeButton.setWidth(310);
        this.allowCommandsExplanationText = MultilineText.create(this.textRenderer, ALLOW_COMMANDS_EXPLANATION_TEXT,
                308);

        // Online Mode button
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.onlineMode).build(this.width / 2 - 155, 124, 150,
                20, ONLINE_MODE_TEXT, (button, onlineMode) -> {
                    this.onlineMode = onlineMode;
                }));
        // PvP Enabled button
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.pvpEnabled).build(this.width / 2 + 5, 124, 150, 20,
                PVP_ENABLED_TEXT, (button, pvpEnabled) -> {
                    this.pvpEnabled = pvpEnabled;
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
        this.addDrawableChild(portField);

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
        this.addDrawableChild(maxPlayersField);

        // MOTD field
        this.motdField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 54, 308, 20,
                MOTD_TEXT);
        motdField.setMaxLength(59); // https://minecraft.fandom.com/wiki/Server.properties#motd
        motdField.setText(server.getServerMotd());
        this.addDrawableChild(motdField);

        // Replace the Start LAN World button with a Start/Save button.
        this.remove(this.children().get(1));
        this.startSaveButton = this.addDrawableChild(
                new ButtonWidget(this.width / 2 - 155, this.height - 28, alreadyOpenedToLan ? 73 : 150, 20,
                        alreadyOpenedToLan ? SAVE_TEXT : START_TEXT, button -> this.startOrSave()));

        if (alreadyOpenedToLan) {
            this.addDrawableChild(
                    new ButtonWidget(this.width / 2 - 78, this.height - 28, 73, 20, STOP_TEXT, button -> this.stop()));
        }

        // Move the Cancel button to the end for consistent Tab order.
        ButtonWidget cancelButton = (ButtonWidget) this.children().get(1);
        this.remove(cancelButton);
        this.addDrawableChild(cancelButton);
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
        this.client.setScreen(null);

        IntegratedServer server = this.client.getServer();
        PlayerManager playerManager = server.getPlayerManager();

        server.setOnlineMode(this.onlineMode);
        server.setPvpEnabled(this.pvpEnabled);

        ((PlayerManagerAccessor) playerManager).setMaxPlayers(this.maxPlayers);

        String oldMotd = server.getServerMotd();
        String motd = this.motdField.getText();
        server.setMotd(motd);
        // Metadata doesn't get updated automatically.
        server.getServerMetadata().setDescription(Text.literal(motd));

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
                            Text.translatable(PUBLISH_PORT_CHANGE_FAILED_TEXT, new Object[] { oldPort }));
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

            server.setDefaultGameMode(this.gameMode);
            // The players' permissions may have changed, so send the new command trees.
            for (ServerPlayerEntity player : playerManager.getPlayerList()) {
                playerManager.sendCommandTree(player); // Do not use server.getCommandManager().sendCommandTree(player)
                                                       // directly or things like the gamemode switcher will not update!
            }
            this.client.inGameHud.getChatHud().addMessage(PUBLISH_SAVED_TEXT);
        } else {
            Text message;
            if (server.openToLan(this.gameMode, false, this.port)) {
                server.setDefaultGameMode(this.gameMode); // Prevents the gamemode from being forced.
                message = Text.translatable(PUBLISH_STARTED_TEXT, new Object[] { this.port });
            } else {
                message = PUBLISH_FAILED_TEXT;
            }
            this.client.inGameHud.getChatHud().addMessage(message);
            this.client.updateWindowTitle(); // Updates the window title to have " - Multiplayer (LAN)".
        }
    }

    private void stop() {
        this.client.setScreen(null);

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
