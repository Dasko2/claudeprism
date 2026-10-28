package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Différences d'API entre versions de Minecraft (groupe : mid). */
public final class Compat {
    private Compat() {}
    public static long handle(MinecraftClient c) { return c.getWindow().getHandle(); }
    public static boolean keyPressed(MinecraftClient c, int code) { return InputUtil.isKeyPressed(c.getWindow().getHandle(), code); }
    public static void push(DrawContext ctx, int x, int y, float s) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(s, s);
    }
    public static void pop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }
    public static void overlay(DrawContext ctx, MinecraftClient c, ItemStack st, int x, int y) { ctx.drawStackOverlay(c.textRenderer, st, x, y); }
    public static void raise(DrawContext ctx) { ctx.createNewRootLayer(); }
    public static ButtonWidget button(Text label, Runnable action, int x, int y, int w, int h) {
        return ButtonWidget.builder(label, b -> action.run()).dimensions(x, y, w, h).build();
    }
    public static List<ItemStack> shulkerItems(ItemStack s) {
        List<ItemStack> out = new ArrayList<>();
        var c = s.get(net.minecraft.component.DataComponentTypes.CONTAINER);
        if (c != null) c.streamNonEmpty().forEach(out::add);
        return out;
    }

    public static int fps(MinecraftClient c) { return c.getCurrentFps(); }
    public static String effectName(net.minecraft.entity.effect.StatusEffectInstance i) { return i.getEffectType().value().getName().getString(); }
}
