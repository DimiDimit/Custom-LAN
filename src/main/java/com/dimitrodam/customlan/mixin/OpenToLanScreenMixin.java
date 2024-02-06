package com.dimitrodam.customlan.mixin;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dimitrodam.customlan.CustomLan;
import com.dimitrodam.customlan.CustomLanConfig;
import com.dimitrodam.customlan.CustomLanState;
import com.dimitrodam.customlan.HasRawMotd;
import com.dimitrodam.customlan.LanSettings;

import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.GameMode;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {
    private static final String PUBLISH_STARTED_CUSTOMLAN_TEXT = "commands.publish.started.customlan";
    private static final Text PUBLISH_FAILED_TEXT = Text.translatable("commands.publish.failed");
    private static final String PUBLISH_PORT_CHANGE_FAILED_TEXT = "commands.publish.failed.port_change";
    private static final String PUBLISH_SAVED_TEXT = "commands.publish.saved";
    private static final Text PUBLISH_STOPPED_TEXT = Text.translatable("commands.publish.stopped");
    private static final Text EXPLANATION_TEXT = Text.translatable("lanServer.explanation");
    private static final Text PER_WORLD_TEXT = Text.translatable("lanServer.perWorld");
    private static final Text GLOBAL_TEXT = Text.translatable("lanServer.global");
    private static final Text SYSTEM_TEXT = Text.translatable("lanServer.system");
    private static final Text LOAD_TEXT = Text.translatable("lanServer.load");
    private static final Text LOAD_SYSTEM_TEXT = Text.translatable("lanServer.load.system");
    private static final Text CLEAR_TEXT = Text.translatable("lanServer.clear");
    private static final Text CLEAR_PERWORLD_QUESTION_TEXT = Text.translatable("lanServer.clear.perWorld.question");
    private static final Text CLEAR_GLOBAL_QUESTION_TEXT = Text.translatable("lanServer.clear.global.question");
    private static final Text START_TEXT = Text.translatable("lanServer.start");
    private static final Text SAVE_TEXT = Text.translatable("lanServer.save");
    private static final Text STOP_TEXT = Text.translatable("lanServer.stop");
    private static final Text ONLINE_MODE_TEXT = Text.translatable("lanServer.onlineMode");
    private static final Text PVP_ENABLED_TEXT = Text.translatable("lanServer.pvpEnabled");
    private static final Text MAX_PLAYERS_TEXT = Text.translatable("lanServer.maxPlayers");
    private static final Text INVALID_MAX_PLAYERS_TEXT = Text.translatable("lanServer.maxPlayers.invalid", 0);
    private static final Text MOTD_TEXT = Text.translatable("lanServer.motd");
    private static final Text MOTD_DESCRIPTION_TEXT = Text.translatable("lanServer.motd.description");

    @Shadow
    @Final
    private static Text PORT_TEXT;
    @Shadow
    @Final
    private static int ERROR_TEXT_COLOR;

    private boolean initialized = false;
    private CustomLanState customLanState;

    private ButtonWidget perWorldLoadButton;
    private ButtonWidget perWorldClearButton;
    private ButtonWidget globalLoadButton;
    private ButtonWidget globalClearButton;
    private CyclingButtonWidget<GameMode> gameModeButton;
    private MultilineText explanationText = MultilineText.EMPTY;
    private CyclingButtonWidget<Boolean> onlineModeButton;
    private CyclingButtonWidget<Boolean> pvpEnabledButton;
    private TextFieldWidget maxPlayersField;
    private TextFieldWidget motdField;
    private ButtonWidget startSaveButton;

    @Shadow
    @Nullable
    private TextFieldWidget portField;

    private boolean onlineMode;
    private boolean pvpEnabled;
    private int rawPort;
    private boolean portValid = true;
    private int maxPlayers;
    private boolean maxPlayersValid = true;
    private String rawMotd;

    @Shadow
    private GameMode gameMode;
    @Shadow
    private int port;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    private boolean canStartOrSave() {
        return this.portValid && this.maxPlayersValid;
    }

    private void loadLanSettingsValues(@Nullable LanSettings lanSettings) {
        if (lanSettings == null) {
            return;
        }
        this.gameMode = lanSettings.gameMode;
        this.onlineMode = lanSettings.onlineMode;
        this.pvpEnabled = lanSettings.pvpEnabled;
        this.rawPort = lanSettings.port;
        this.port = lanSettings.port != -1 ? lanSettings.port : NetworkUtils.findLocalPort();
        this.maxPlayers = lanSettings.maxPlayers;
        this.rawMotd = lanSettings.motd;
    }

    private void loadLanSettings(@Nullable LanSettings lanSettings) {
        this.loadLanSettingsValues(lanSettings);
        this.gameModeButton.setValue(this.gameMode);
        this.onlineModeButton.setValue(this.onlineMode);
        this.pvpEnabledButton.setValue(this.pvpEnabled);
        this.portField.setText(this.rawPort != -1 ? Integer.toString(this.port) : "");
        this.maxPlayersField.setText(Integer.toString(this.maxPlayers));
        this.motdField.setText(this.rawMotd);
    }

    private LanSettings saveLanSettings() {
        return new LanSettings(this.gameMode, this.onlineMode, this.pvpEnabled,
                this.portField.getText().isBlank() ? -1 : this.port, this.maxPlayers, this.rawMotd);
    }

    private void updateSettingsButtons() {
        boolean savedPerWorld = this.customLanState.getLanSettings() != null;
        this.perWorldLoadButton.active = savedPerWorld;
        this.perWorldClearButton.active = savedPerWorld;

        boolean savedGlobal = CustomLanConfig.INSTANCE.getLanSettings() != null;
        this.globalLoadButton.active = savedGlobal;
        this.globalClearButton.active = savedGlobal;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void preInit(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();

        // Initialization cannot be done in the constructor because
        // this.client wouldn't have been initialized yet.
        if (!this.initialized) {
            this.customLanState = server.getOverworld().getPersistentStateManager().getOrCreate(CustomLanState::fromNbt,
                    CustomLanState::new, CustomLanState.CUSTOM_LAN_KEY);

            if (server.isRemote()) {
                this.gameMode = server.getDefaultGameMode();
                this.onlineMode = server.isOnlineMode();
                this.pvpEnabled = server.isPvpEnabled();
                this.port = server.getServerPort();
                this.rawPort = this.port;
                this.maxPlayers = server.getMaxPlayerCount();
                this.rawMotd = ((HasRawMotd) server).getRawMotd();
            } else if (this.customLanState.getLanSettings() != null) {
                this.loadLanSettingsValues(this.customLanState.getLanSettings());
            } else if (CustomLanConfig.INSTANCE.getLanSettings() != null) {
                this.loadLanSettingsValues(CustomLanConfig.INSTANCE.getLanSettings());
            } else {
                this.loadLanSettingsValues(LanSettings.systemDefaults(server));
            }

            this.initialized = true;
        }

        // Per-world Save button
        this.addDrawableChild(ButtonWidget.builder(SAVE_TEXT,
                button -> {
                    this.customLanState.setLanSettings(this.saveLanSettings());
                    this.updateSettingsButtons();
                }).dimensions(this.width / 2 - 77, 8, 74, 20).build());
        // Per-world Load button
        this.perWorldLoadButton = this.addDrawableChild(ButtonWidget.builder(LOAD_TEXT,
                button -> this.loadLanSettings(this.customLanState.getLanSettings()))
                .dimensions(this.width / 2 + 2, 8, 74, 20).build());
        // Per-world Clear button
        this.perWorldClearButton = this.addDrawableChild(ButtonWidget.builder(CLEAR_TEXT,
                button -> this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        this.customLanState.setLanSettings(null);
                        this.updateSettingsButtons();
                    }
                    this.client.setScreen(this);
                }, CLEAR_PERWORLD_QUESTION_TEXT, ScreenTexts.EMPTY, CLEAR_TEXT, ScreenTexts.CANCEL)))
                .dimensions(this.width / 2 + 81, 8, 74, 20).build());

        // Global Save button
        this.addDrawableChild(ButtonWidget.builder(SAVE_TEXT,
                button -> {
                    CustomLanConfig.INSTANCE.setLanSettings(this.saveLanSettings());
                    this.updateSettingsButtons();
                }).dimensions(this.width / 2 - 77, 32, 74, 20).build());
        // Global Load button
        this.globalLoadButton = this.addDrawableChild(ButtonWidget.builder(LOAD_TEXT,
                // Reload the config before loading the settings to allow for hot swapping.
                button -> this.loadLanSettings(CustomLanConfig.reload().getLanSettings()))
                .dimensions(this.width / 2 + 2, 32, 74, 20).build());
        // Global Clear button
        this.globalClearButton = this.addDrawableChild(ButtonWidget.builder(CLEAR_TEXT,
                button -> this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        CustomLanConfig.INSTANCE.setLanSettings(null);
                        this.updateSettingsButtons();
                    }
                    this.client.setScreen(this);
                }, CLEAR_GLOBAL_QUESTION_TEXT, ScreenTexts.EMPTY, CLEAR_TEXT, ScreenTexts.CANCEL)))
                .dimensions(this.width / 2 + 81, 32, 74, 20).build());

        // Load system settings button
        this.addDrawableChild(ButtonWidget.builder(LOAD_SYSTEM_TEXT,
                button -> this.loadLanSettings(LanSettings.systemDefaults(server)))
                .dimensions(this.width / 2 - 77, 56, 232, 20).build());

        this.updateSettingsButtons();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "init", at = @At("TAIL"))
    private void postInit(CallbackInfo ci) {
        IntegratedServer server = this.client.getServer();
        boolean alreadyOpenedToLan = server.isRemote();

        // Replace the Allow Cheats button
        // with explanation text below it (added in renderText)
        // and have the Game Mode button fill its place.
        this.remove(this.children().get(8));
        this.gameModeButton = (CyclingButtonWidget<GameMode>) this.children().get(7);
        this.gameModeButton.setWidth(310);
        this.explanationText = MultilineText.create(this.textRenderer, EXPLANATION_TEXT, 308);

        // Online Mode button
        this.onlineModeButton = this.addDrawableChild(
                CyclingButtonWidget.onOffBuilder(this.onlineMode).build(this.width / 2 - 155, 124, 150,
                        20, ONLINE_MODE_TEXT, (button, onlineMode) -> {
                            this.onlineMode = onlineMode;
                        }));
        // PvP Enabled button
        this.pvpEnabledButton = this.addDrawableChild(
                CyclingButtonWidget.onOffBuilder(this.pvpEnabled).build(this.width / 2 + 5, 124, 150, 20,
                        PVP_ENABLED_TEXT, (button, pvpEnabled) -> {
                            this.pvpEnabled = pvpEnabled;
                        }));

        // Update the Port field and re-add it for consistent Tab order.
        this.portField.setPos(this.width / 2 - 154, this.height - 92);
        this.portField.setWidth(148);
        this.portField.setText(this.rawPort != -1 ? Integer.toString(this.port) : "");
        this.remove(this.portField);
        this.addDrawableChild(this.portField);

        // Max Players field
        this.maxPlayersField = new TextFieldWidget(this.textRenderer, this.width / 2 + 6, this.height - 92,
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
                maxPlayersField.setEditableColor(TextFieldWidget.DEFAULT_EDITABLE_COLOR);
                maxPlayersField.setTooltip(null);
            } else {
                this.maxPlayersValid = false;
                maxPlayersField.setEditableColor(ERROR_TEXT_COLOR);
                maxPlayersField.setTooltip(Tooltip.of(INVALID_MAX_PLAYERS_TEXT));
            }
            this.startSaveButton.active = this.canStartOrSave();
        });
        this.addDrawableChild(maxPlayersField);

        // MOTD field
        this.motdField = new TextFieldWidget(this.textRenderer, this.width / 2 - 154, this.height - 54, 308, 20,
                MOTD_TEXT);
        motdField.setMaxLength(59); // https://minecraft.wiki/w/Server.properties#motd
        motdField.setText(this.rawMotd);
        motdField.setTooltip(
                Tooltip.of(Text.literal(CustomLan.processMotd(server, MOTD_DESCRIPTION_TEXT.getString()))));
        motdField.setChangedListener(rawMotd -> this.rawMotd = rawMotd);
        this.addDrawableChild(motdField);

        // Replace the Start LAN World button with a Start/Save button.
        this.remove(this.children().get(8));
        this.startSaveButton = this.addDrawableChild(
                ButtonWidget.builder(alreadyOpenedToLan ? SAVE_TEXT : START_TEXT, button -> this.startOrSave())
                        .dimensions(this.width / 2 - 155, this.height - 28, alreadyOpenedToLan ? 73 : 150, 20).build());
        this.startSaveButton.active = this.canStartOrSave();

        if (alreadyOpenedToLan) {
            this.addDrawableChild(ButtonWidget.builder(STOP_TEXT, button -> this.stop())
                    .dimensions(this.width / 2 - 78, this.height - 28, 73, 20).build());
        }

        // Re-add the Cancel button for consistent Tab order.
        ButtonWidget cancelButton = (ButtonWidget) this.children().get(8);
        this.remove(cancelButton);
        this.addDrawableChild(cancelButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Per-world settings text
        drawTextWithShadow(matrices, this.textRenderer, PER_WORLD_TEXT, this.width / 2 - 155, 14, 0xFFFFFF);
        // Global settings text
        drawTextWithShadow(matrices, this.textRenderer, GLOBAL_TEXT, this.width / 2 - 155, 38, 0xFFFFFF);
        // System settings text
        drawTextWithShadow(matrices, this.textRenderer, SYSTEM_TEXT, this.width / 2 - 155, 62, 0xFFFFFF);

        // Explanation text
        this.explanationText.drawWithShadow(matrices, this.width / 2 - 154, 148, 9, 0xA0A0A0);

        // Port field text
        drawTextWithShadow(matrices, this.textRenderer, PORT_TEXT, this.width / 2 - 154, this.height - 104, 0xA0A0A0);
        // Max Players field text
        drawTextWithShadow(matrices, this.textRenderer, MAX_PLAYERS_TEXT, this.width / 2 + 6, this.height - 104,
                0xA0A0A0);
        // MOTD field text
        drawTextWithShadow(matrices, this.textRenderer, MOTD_TEXT, this.width / 2 - 154, this.height - 66, 0xA0A0A0);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        this.maxPlayersField.tick();
        this.motdField.tick();
    }

    @Redirect(method = "updatePort", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/NetworkUtils;isPortAvailable(I)Z"))
    private boolean allowKeepingPort(int port) {
        return NetworkUtils.isPortAvailable(port) || port == this.client.getServer().getServerPort();
    }

    @Inject(method = "method_47416", at = @At("TAIL"), remap = false)
    private void updatePortValid(ButtonWidget button, String portText, CallbackInfo ci) {
        this.portValid = button.active;
        if (this.startSaveButton != null) {
            this.startSaveButton.active = this.canStartOrSave();
        }
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 50))
    private int changeTitleY(int y) {
        return 88;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/OpenToLanScreen;drawCenteredText(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/OpenToLanScreen;drawCenteredText(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", ordinal = 1)))
    private void removeOtherPlayersAndPortText(MatrixStack matrices, TextRenderer textRenderer, Text text,
            int centerX, int y, int color) {
    }

    private void startOrSave() {
        this.client.setScreen(null);

        CustomLan.startOrSaveLan(this.client.getServer(), this.gameMode,
                this.onlineMode, this.pvpEnabled, this.port, this.maxPlayers, this.rawMotd,
                motd -> this.client.inGameHud.getChatHud()
                        .addMessage(Text.translatable(PUBLISH_STARTED_CUSTOMLAN_TEXT,
                                Texts.bracketedCopyable(String.valueOf(this.port)), motd)),
                motd -> this.client.inGameHud.getChatHud()
                        .addMessage(Text.translatable(PUBLISH_SAVED_TEXT,
                                Texts.bracketedCopyable(String.valueOf(this.port)), motd)),
                () -> this.client.inGameHud.getChatHud().addMessage(PUBLISH_FAILED_TEXT),
                oldPort -> this.client.inGameHud.getChatHud().addMessage(Text.translatable(
                        PUBLISH_PORT_CHANGE_FAILED_TEXT, Texts.bracketedCopyable(String.valueOf(oldPort)))));
    }

    private void stop() {
        this.client.setScreen(null);

        CustomLan.stopLan(this.client.getServer());

        this.client.inGameHud.getChatHud().addMessage(PUBLISH_STOPPED_TEXT);
    }

    /**
     * Allow starting/saving with Enter as well as the Start/Save button.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.canStartOrSave()) {
                this.startOrSave();
            }
            return true;
        }
        return false;
    }
}
