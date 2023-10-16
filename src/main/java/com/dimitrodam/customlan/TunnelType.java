package com.dimitrodam.customlan;

import org.jetbrains.annotations.Nullable;

import com.dimitrodam.customlan.util.Utils;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;

import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.StringIdentifiable;

public enum TunnelType implements StringIdentifiable {
    NONE("none"),
    NGROK("ngrok") {
        @Override
        public @Nullable Text start(MinecraftServer server) throws TunnelException {
            String authtoken = CustomLan.CONFIG.getConfig().ngrokAuthtoken.strip();
            if (authtoken.isEmpty()) {
                throw new TunnelException(ScreenTexts.composeGenericOptionText(NGROK_FAILED,
                        Text.translatable(NGROK_FAILED_NO_AUTHTOKEN, Utils.createLink(NGROK_AUTHTOKEN_URL))));
            }

            try {
                CustomLan.NGROK_CLIENT = new NgrokClient.Builder()
                        .withJavaNgrokConfig(new JavaNgrokConfig.Builder()
                                .withAuthToken(authtoken)
                                .build())
                        .build();

                Tunnel tunnel = CustomLan.NGROK_CLIENT.connect(new CreateTunnel.Builder()
                        .withProto(Proto.TCP)
                        .withAddr(server.getServerPort())
                        .build());

                return Texts.bracketedCopyable(tunnel.getPublicUrl().replaceFirst("^tcp:\\/\\/", ""));
            } catch (Exception e) {
                e.printStackTrace();
                throw new TunnelException(
                        ScreenTexts.composeGenericOptionText(NGROK_FAILED, Text.of(e.getMessage())), e);
            }
        }

        @Override
        public void stop(MinecraftServer server) throws TunnelException {
            try {
                if (CustomLan.NGROK_CLIENT != null) {
                    CustomLan.NGROK_CLIENT.kill();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new TunnelException(
                        ScreenTexts.composeGenericOptionText(NGROK_STOP_FAILED, Text.of(e.getMessage())), e);
            }
        }
    };

    public static class TunnelException extends Exception {
        private final Text messageText;

        public TunnelException(Text messageText) {
            super(messageText.getString());
            this.messageText = messageText;
        }

        public TunnelException(Text messageText, Throwable cause) {
            super(messageText.toString(), cause);
            this.messageText = messageText;
        }

        public Text getMessageText() {
            return this.messageText;
        }
    }

    @SuppressWarnings("deprecation")
    public static final StringIdentifiable.EnumCodec<TunnelType> CODEC = StringIdentifiable
            .createCodec(TunnelType::values);

    private static final String NGROK_AUTHTOKEN_URL = "https://dashboard.ngrok.com/get-started/your-authtoken";

    private static final Text NGROK_FAILED = Text.translatable("commands.publish.failed.tunnel.ngrok");
    private static final Text NGROK_STOP_FAILED = Text.translatable("commands.publish.failed.tunnel.ngrok.stop");
    private static final String NGROK_FAILED_NO_AUTHTOKEN = "commands.publish.failed.tunnel.ngrok.noAuthtoken";

    private final String name;

    public static TunnelType byName(String name) {
        return byName(name, NONE);
    }

    @Nullable
    public static TunnelType byName(String name, @Nullable TunnelType defaultTunnelType) {
        @SuppressWarnings("deprecation")
        TunnelType tunnelType = CODEC.byId(name);
        return tunnelType != null ? tunnelType : defaultTunnelType;
    }

    private TunnelType(String name) {
        this.name = name;
    }

    @Nullable
    public Text start(MinecraftServer server) throws TunnelException {
        return null;
    }

    public void stop(MinecraftServer server) throws TunnelException {
    }

    public String getName() {
        return this.name;
    }

    public Text getTranslatableName() {
        return Text.translatable("customlan.tunnel." + this.name);
    }

    @Override
    public String asString() {
        return this.name;
    }
}
