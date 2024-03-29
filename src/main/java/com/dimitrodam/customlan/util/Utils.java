package com.dimitrodam.customlan.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Utils {
    private Utils() {
    }

    public static Text createLink(String url) {
        return Text.literal(url).styled(style -> style.withFormatting(Formatting.BLUE, Formatting.UNDERLINE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
    }
}
