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
    public static final int KEY_N = InputConstants.KEY_N;
    public static final int KEY_C = InputConstants.KEY_C;
    public static final int KEY_V = InputConstants.KEY_V;
    public static final int KEY_RSHIFT = InputConstants.KEY_RSHIFT;
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    private static final int[] CAND = {
        InputConstants.KEY_A, InputConstants.KEY_B, InputConstants.KEY_C, InputConstants.KEY_D, InputConstants.KEY_E, InputConstants.KEY_F, InputConstants.KEY_G, InputConstants.KEY_H, InputConstants.KEY_I, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_M, InputConstants.KEY_N, InputConstants.KEY_O, InputConstants.KEY_P, InputConstants.KEY_Q, InputConstants.KEY_R, InputConstants.KEY_S, InputConstants.KEY_T, InputConstants.KEY_U, InputConstants.KEY_V, InputConstants.KEY_W, InputConstants.KEY_X, InputConstants.KEY_Y, InputConstants.KEY_Z, InputConstants.KEY_0, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9, InputConstants.KEY_F1, InputConstants.KEY_F2, InputConstants.KEY_F3, InputConstants.KEY_F4, InputConstants.KEY_F5, InputConstants.KEY_F6, InputConstants.KEY_F7, InputConstants.KEY_F8, InputConstants.KEY_F9, InputConstants.KEY_F10, InputConstants.KEY_F11, InputConstants.KEY_F12, InputConstants.KEY_SPACE, InputConstants.KEY_TAB, InputConstants.KEY_RETURN, InputConstants.KEY_UP, InputConstants.KEY_DOWN, InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT, InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT, InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL, InputConstants.KEY_LALT, InputConstants.KEY_RALT
    };
    public static int[] candidates() { return CAND; }
    public static void raise(GuiGraphicsExtractor ctx) { ctx.nextStratum(); }
}
