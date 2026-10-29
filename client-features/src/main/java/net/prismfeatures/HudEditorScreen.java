package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Éditeur de placement : glisser chaque élément avec la souris où on veut. */
public class HudEditorScreen extends ScreenBase {
    private final Screen parent;
    private Hud.Element dragging = null;
    private Hud.Element selected = Hud.ELEMENTS.get(0);
    private int grabDx = 0;
    private int grabDy = 0;
    private boolean wasDown = false;
    private Hud.Element resizing = null;
    private int rRight = 0;
    private int rBottom = 0;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Placer les éléments"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        final int bw = 90;
        final int y = this.height - 28;
        final int x0 = this.width / 2 - (2 * bw + 4) / 2;
        this.addDrawableChild(Compat.button(Text.literal("Réinitialiser"), () -> Config.resetPositions(), x0, y, bw, 20));
        this.addDrawableChild(Compat.button(Text.literal("Terminé"), () -> this.close(), x0 + (bw + 4), y, bw, 20));
    }

    @Override
    protected void draw(Gfx ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient c = MinecraftClient.getInstance();
        ctx.centered(Text.literal("Glissez un élément pour le déplacer, tirez le carré en haut à gauche pour changer sa taille"), this.width / 2, 12, 0xFFFFFFFF);

        // état de la souris (sans dépendre de la signature des événements de souris)
        boolean down = GLFW.glfwGetMouseButton(Compat.handle(c), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown && mouseY < this.height - 36) {
            for (Hud.Element e : Hud.ELEMENTS) {
                int hx = e.x(this.width, c, true);
                int hy = e.y(this.height, c, true);
                if (mouseX >= hx - 7 && mouseX <= hx + 3 && mouseY >= hy - 7 && mouseY <= hy + 3) {
                    resizing = e; // carré de redimensionnement (haut gauche) ; le coin bas droit reste fixe
                    selected = e;
                    rRight = hx + e.w(c);
                    rBottom = hy + e.h(c);
                    dragging = null;
                    break;
                }
            }
        }
        if (down && !wasDown && resizing == null && mouseY < this.height - 36) {
            for (Hud.Element e : Hud.ELEMENTS) {
                int x = e.x(this.width, c, true);
                int y = e.y(this.height, c, true);
                if (mouseX >= x - 2 && mouseX <= x + e.w(c) + 2 && mouseY >= y - 2 && mouseY <= y + e.h(c) + 2) {
                    dragging = e; // le dernier de la liste est « au-dessus »
                    selected = e;
                    grabDx = mouseX - x;
                    grabDy = mouseY - y;
                }
            }
        }
        if (down && resizing != null) {
            Hud.Element e = resizing;
            double baseW = Math.max(1, e.width(c, true));
            double baseH = Math.max(1, e.height(c, true));
            double ns = (((rRight - mouseX) / baseW) + ((rBottom - mouseY) / baseH)) / 2.0;
            Config.setScaleLive(e.id, ns, false);
            int w = e.w(c);
            int h = e.h(c);
            double fx = Math.min(1.0, Math.max(0.0, (rRight - w) / (double) Math.max(1, this.width - w)));
            double fy = Math.min(1.0, Math.max(0.0, (rBottom - h) / (double) Math.max(1, this.height - h)));
            Config.setPos(e.id, fx, fy, false);
        }
        if (!down && wasDown && resizing != null) {
            double[] p = Config.pos(resizing.id, 0, 0);
            Config.setScaleLive(resizing.id, Config.scale(resizing.id), true);
            Config.setPos(resizing.id, p[0], p[1], true);
            resizing = null;
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
            boolean active = e == selected;
            int color = active ? 0xFF7C5CFF : 0x887C5CFF;
            ctx.fill(x - 3, y - 3, x + w + 3, y + h + 3, active ? 0x557C5CFF : 0x33000000);
            ctx.fill(x - 3, y - 3, x + w + 3, y - 2, color);
            ctx.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, color);
            ctx.fill(x - 3, y - 3, x - 2, y + h + 3, color);
            ctx.fill(x + w + 2, y - 3, x + w + 3, y + h + 3, color);
            ctx.fill(x - 7, y - 7, x + 3, y + 3, 0xFFFFFFFF);
            ctx.fill(x - 6, y - 6, x + 2, y + 2, color);
            e.drawPreview(ctx, c, x, y);
            ctx.text(e.label, x, y - 13, 0xFFBBAAFF);
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
