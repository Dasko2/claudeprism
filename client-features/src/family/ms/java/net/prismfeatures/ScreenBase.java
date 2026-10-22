package net.prismfeatures;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** Base des écrans du mod : relie le dessin natif à Gfx. */
public abstract class ScreenBase extends Screen {
    protected ScreenBase(Text title) { super(title); }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        draw(new Gfx(matrices), mouseX, mouseY, delta);
    }

    protected abstract void draw(Gfx g, int mouseX, int mouseY, float delta);
}
