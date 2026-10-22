package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Surface de dessin (famille DrawContext, Minecraft 1.20+). */
public final class Gfx {
    private final DrawContext c;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public Gfx(DrawContext c) { this.c = c; }

    public void fill(int x1, int y1, int x2, int y2, int color) { c.fill(x1, y1, x2, y2, color); }
    public void text(String s, int x, int y, int color) { c.drawTextWithShadow(mc.textRenderer, s, x, y, color); }
    public void centered(Text t, int x, int y, int color) { c.drawCenteredTextWithShadow(mc.textRenderer, t, x, y, color); }
    public void item(ItemStack s, int x, int y) { c.drawItem(s, x, y); }
    public void overlay(ItemStack s, int x, int y) { Compat.overlay(c, mc, s, x, y); }
    public void push(int x, int y, float s) { Compat.push(c, x, y, s); }
    public void pop() { Compat.pop(c); }
    public void raise() { Compat.raise(c); }
    public int width() { return c.getScaledWindowWidth(); }
    public int height() { return c.getScaledWindowHeight(); }
}
