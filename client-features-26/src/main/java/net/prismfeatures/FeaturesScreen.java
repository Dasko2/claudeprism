package net.prismfeatures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu de config en jeu (dessiné par MenuUi). */
public class FeaturesScreen extends Screen {
    private final Screen parent;
    private final MenuUi ui = new MenuUi(() -> Compat.setScreen(this.minecraft, new HudEditorScreen(this)), () -> this.onClose());

    public FeaturesScreen() {
        this(null);
    }

    public FeaturesScreen(Screen parent) {
        super(Component.literal("Menu de config"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        PrismFeatures.safe("menu-ui", () -> ui.draw(new Gfx(context), mouseX, mouseY));
    }

    @Override
    public void onClose() {
        PrismFeatures.listening = null;
        PrismFeatures.onBindingChanged = null;
        if (this.minecraft != null) Compat.setScreen(this.minecraft, parent);
    }
}
