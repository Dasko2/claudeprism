package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

/** Différences d'API entre versions de Minecraft (groupe : mid). */
final class Compat {
    private Compat() {}
    static long handle(MinecraftClient c) { return c.getWindow().getHandle(); }
    static boolean keyPressed(MinecraftClient c, int code) { return InputUtil.isKeyPressed(c.getWindow().getHandle(), code); }
    static void push(DrawContext ctx, int x, int y, float s) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
    }
    static void pop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }
    static void overlay(DrawContext ctx, MinecraftClient c, net.minecraft.item.ItemStack st, int x, int y) { ctx.drawStackOverlay(c.textRenderer, st, x, y); }
}
