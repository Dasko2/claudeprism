package net.prismfeatures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Petit adaptateur pour que Extras reste identique entre les arbres de code. */
public final class Gfx {
    private final GuiGraphicsExtractor c;
    private final Minecraft mc = Minecraft.getInstance();

    public Gfx(GuiGraphicsExtractor c) { this.c = c; }

    public void fill(int x1, int y1, int x2, int y2, int color) { c.fill(x1, y1, x2, y2, color); }
    public void text(String s, int x, int y, int color) { c.text(mc.font, s, x, y, color, true); }
    public int width() { return c.guiWidth(); }
    public int height() { return c.guiHeight(); }
}
