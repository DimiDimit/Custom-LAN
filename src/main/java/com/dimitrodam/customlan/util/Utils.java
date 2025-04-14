package com.dimitrodam.customlan.util;

import java.net.URI;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Utils {
    private Utils() {
    }

    public static Text createLink(URI uri) {
        return Text.literal(uri.toString()).styled(style -> style.withFormatting(Formatting.BLUE, Formatting.UNDERLINE)
                .withClickEvent(new ClickEvent.OpenUrl(uri)));
    }
}
