package net.prismfeatures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Éditeur de placement : glisser chaque élément avec la souris où on veut. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private Hud.Element dragging = null;
    private Hud.Element selected = Hud.ELEMENTS.get(0);
    private int grabDx = 0;
    private int grabDy = 0;
    private boolean wasDown = false;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Placer les éléments"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        final int bw = 90;
        final int y = this.height - 28;
        final int x0 = this.width / 2 - (4 * bw + 3 * 4) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Taille -"), b -> Config.setScale(selected.id, selected.scale() - 0.1))
                .bounds(x0, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Taille +"), b -> Config.setScale(selected.id, selected.scale() + 0.1))
                .bounds(x0 + (bw + 4), y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Réinitialiser"), b -> Config.resetPositions())
                .bounds(x0 + 2 * (bw + 4), y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Terminé"), b -> this.onClose())
                .bounds(x0 + 3 * (bw + 4), y, bw, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        Minecraft c = Minecraft.getInstance();
        ctx.centeredText(this.font, Component.literal("Glissez chaque élément à la souris. Taille -/+ agit sur l'élément sélectionné : " + selected.label + " (x" + String.format("%.1f", selected.scale()) + ")"), this.width / 2, 12, 0xFFFFFFFF);

        // état de la souris (sans dépendre de la signature des événements de souris)
        boolean down = Compat.leftMouseDown(c);
        if (down && !wasDown && mouseY < this.height - 36) {
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
            e.drawPreview(ctx, c, x, y);
            ctx.text(this.font, e.label, x, y - 13, 0xFFBBAAFF, true);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) Compat.setScreen(this.minecraft, parent);
    }
}
