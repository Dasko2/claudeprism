package net.prismfeatures;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Menu de config en jeu (dessiné par MenuUi). */
public class FeaturesScreen extends ScreenBase {
    private final Screen parent;
    private final MenuUi ui = new MenuUi(() -> this.client.setScreen(new HudEditorScreen(this)), () -> this.close());

    public FeaturesScreen() {
        this(null);
    }

    public FeaturesScreen(Screen parent) {
        super(Text.literal("Menu de config"));
        this.parent = parent;
    }

    @Override
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        PrismFeatures.safe("menu-ui", () -> ui.draw(g, mouseX, mouseY));
    }

    @Override
    public void close() {
        PrismFeatures.listening = null;
        PrismFeatures.onBindingChanged = null;
        if (this.client != null) this.client.setScreen(parent);
    }
}
