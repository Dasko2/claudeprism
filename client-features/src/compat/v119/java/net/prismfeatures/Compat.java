package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/** Différences d'API entre versions de Minecraft (groupe : v119). */
public final class Compat {
    private Compat() {}
    public static long handle(MinecraftClient c) { return c.getWindow().getHandle(); }
    public static boolean keyPressed(MinecraftClient c, int code) { return InputUtil.isKeyPressed(c.getWindow().getHandle(), code); }
    public static void item(MatrixStack m, MinecraftClient c, ItemStack st, int x, int y) { c.getItemRenderer().renderGuiItemIcon(st, x, y); }
    public static void overlay(MatrixStack m, MinecraftClient c, ItemStack st, int x, int y) { c.getItemRenderer().renderGuiItemOverlay(c.textRenderer, st, x, y); }
    public static ButtonWidget button(Text label, Runnable action, int x, int y, int w, int h) {
        return new ButtonWidget(x, y, w, h, label, b -> action.run());
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

}
