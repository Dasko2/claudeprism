package net.prismfeatures;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Surface de dessin (famille MatrixStack, Minecraft 1.19.x). */
public final class Gfx {
    private final MatrixStack m;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public Gfx(MatrixStack m) { this.m = m; }

    public void fill(int x1, int y1, int x2, int y2, int color) { DrawableHelper.fill(m, x1, y1, x2, y2, color); }
    public void text(String s, int x, int y, int color) { mc.textRenderer.drawWithShadow(m, s, (float) x, (float) y, color); }
    public void centered(Text t, int x, int y, int color) { mc.textRenderer.drawWithShadow(m, t, x - mc.textRenderer.getWidth(t) / 2f, (float) y, color); }
    public void item(ItemStack s, int x, int y) { Compat.item(m, mc, s, x, y); }
    public void overlay(ItemStack s, int x, int y) { Compat.overlay(m, mc, s, x, y); }
    /** L'échelle passe par la matrice globale : c'est elle qu'utilise le rendu des objets dans ces versions. */
    public void push(int x, int y, float s) {
        MatrixStack mv = RenderSystem.getModelViewStack();
        mv.push();
        mv.translate((float) x, (float) y, 0f);
        mv.scale(s, s, 1f);
        RenderSystem.applyModelViewMatrix();
    }
    public void pop() {
        RenderSystem.getModelViewStack().pop();
        RenderSystem.applyModelViewMatrix();
    }
    public void raise() { }
    public int width() { return mc.getWindow().getScaledWidth(); }
    public int height() { return mc.getWindow().getScaledHeight(); }
}
