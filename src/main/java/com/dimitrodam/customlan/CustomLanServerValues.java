package com.dimitrodam.customlan;

import net.minecraft.text.Text;

public interface CustomLanServerValues {
    public TunnelType getTunnelType();

    public void setTunnelType(TunnelType tunnelType);

    public Text getTunnelText();

    public void setTunnelText(Text tunnelText);

    public String getRawMotd();

    public void setRawMotd(String rawMotd);
}
