package net.prismfeatures;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** Couche d'accès aux API de Minecraft 26.x (noms Mojang). */
final class Compat {
    private Compat() {}
    static long handle(Minecraft c) { return c.getWindow().handle(); }
    static boolean keyPressed(Minecraft c, int code) { return InputConstants.isKeyDown(c.getWindow(), code); }
    static void push(GuiGraphicsExtractor ctx, int x, int y, float s) {
        var m = ctx.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
    }
    static void pop(GuiGraphicsExtractor ctx) { ctx.pose().popMatrix(); }
    static void overlay(GuiGraphicsExtractor ctx, Minecraft c, ItemStack st, int x, int y) { ctx.itemDecorations(c.font, st, x, y); }
}
