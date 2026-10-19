package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Éditeur de placement : glisser chaque élément avec la souris où on veut. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private Hud.Element dragging = null;
    private int grabDx = 0;
    private int grabDy = 0;
    private boolean wasDown = false;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Placer les éléments"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Terminé"), b -> this.close())
                .dimensions(this.width / 2 - 104, this.height - 28, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Réinitialiser"), b -> Config.resetPositions())
                .dimensions(this.width / 2 + 4, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        MinecraftClient c = MinecraftClient.getInstance();
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Glissez chaque élément avec la souris à l'endroit voulu"), this.width / 2, 12, 0xFFFFFFFF);

        // état de la souris (sans dépendre de la signature des événements de souris)
        boolean down = GLFW.glfwGetMouseButton(c.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown && mouseY < this.height - 36) {
            for (Hud.Element e : Hud.ELEMENTS) {
                int x = e.x(this.width, c, true);
                int y = e.y(this.height, c, true);
                if (mouseX >= x - 2 && mouseX <= x + e.w(c) + 2 && mouseY >= y - 2 && mouseY <= y + e.h(c) + 2) {
                    dragging = e; // le dernier de la liste est « au-dessus »
                    grabDx = mouseX - x;
                    grabDy = mouseY - y;
                }
            }
        }
        if (down && dragging != null) {
            int freeW = Math.max(1, this.width - dragging.w(c));
            int freeH = Math.max(1, this.height - dragging.h(c));
            double fx = Math.min(1.0, Math.max(0.0, (mouseX - grabDx) / (double) freeW));
            double fy = Math.min(1.0, Math.max(0.0, (mouseY - grabDy) / (double) freeH));
            Config.setPos(dragging.id, fx, fy, false);
        }
        if (!down && wasDown && dragging != null) {
            double[] p = Config.pos(dragging.id, 0, 0);
            Config.setPos(dragging.id, p[0], p[1], true);
            dragging = null;
        }
        wasDown = down;

        for (Hud.Element e : Hud.ELEMENTS) {
            int x = e.x(this.width, c, true);
            int y = e.y(this.height, c, true);
            int w = e.w(c);
            int h = e.h(c);
            boolean active = e == dragging;
            int color = active ? 0xFF7C5CFF : 0x887C5CFF;
            ctx.fill(x - 3, y - 3, x + w + 3, y + h + 3, active ? 0x557C5CFF : 0x33000000);
            ctx.fill(x - 3, y - 3, x + w + 3, y - 2, color);
            ctx.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, color);
            ctx.fill(x - 3, y - 3, x - 2, y + h + 3, color);
            ctx.fill(x + w + 2, y - 3, x + w + 3, y + h + 3, color);
            e.drawPreview(ctx, c, x, y);
            ctx.drawTextWithShadow(this.textRenderer, e.label, x, y - 13, 0xFFBBAAFF);
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
