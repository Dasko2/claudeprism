package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Différences d'API entre versions de Minecraft (groupe : v120). */
public final class Compat {
    private Compat() {}
    public static long handle(MinecraftClient c) { return c.getWindow().getHandle(); }
    public static boolean keyPressed(MinecraftClient c, int code) { return InputUtil.isKeyPressed(c.getWindow().getHandle(), code); }
    public static void push(DrawContext ctx, int x, int y, float s) {
        var m = ctx.getMatrices();
        m.push();
        m.translate((float) x, (float) y, 0f);
        m.scale(s, s, 1f);
    }
    public static void pop(DrawContext ctx) { ctx.getMatrices().pop(); }
    public static void overlay(DrawContext ctx, MinecraftClient c, ItemStack st, int x, int y) { ctx.drawItemInSlot(c.textRenderer, st, x, y); }
    public static void raise(DrawContext ctx) { }
    public static ButtonWidget button(Text label, Runnable action, int x, int y, int w, int h) {
        return ButtonWidget.builder(label, b -> action.run()).dimensions(x, y, w, h).build();
    }
    public static List<ItemStack> shulkerItems(ItemStack s) {
        List<ItemStack> out = new ArrayList<>();
        net.minecraft.nbt.NbtCompound tag = s.getSubNbt("BlockEntityTag");
        if (tag == null || !tag.contains("Items", 9)) return out;
        net.minecraft.nbt.NbtList list = tag.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            ItemStack st = ItemStack.fromNbt(list.getCompound(i));
            if (!st.isEmpty()) out.add(st);
        }
        return out;
    }

    public static int fps(MinecraftClient c) { return c.getCurrentFps(); }
}
