package net.prismfeatures;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Couche d'accès aux API de Minecraft 26.x (noms Mojang). */
public final class Compat {
    private Compat() {}
    public static boolean keyPressed(Minecraft c, int code) { return InputConstants.isKeyDown(c.getWindow(), code); }
    public static void push(GuiGraphicsExtractor ctx, int x, int y, float s) {
        var m = ctx.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
    }
    public static void pop(GuiGraphicsExtractor ctx) { ctx.pose().popMatrix(); }
    public static void overlay(GuiGraphicsExtractor ctx, Minecraft c, ItemStack st, int x, int y) { ctx.itemDecorations(c.font, st, x, y); }
    public static Screen screen(Minecraft c) { return c.screen; }
    public static void setScreen(Minecraft c, Screen s) { c.setScreen(s); }
    public static void overlayMessage(Minecraft c, Component m) { c.gui.setOverlayMessage(m, false); }
    public static boolean hudHidden(Minecraft c) { return c.options.hideGui; }
    public static boolean leftMouseDown(Minecraft c) { return c.mouseHandler.isLeftPressed(); }
    public static String keyDisplay(int code) { return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString(); }
}
