package net.prismfeatures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FeaturesScreen extends Screen {
    private final Screen parent;
    private final MenuUi ui = new MenuUi(() -> Compat.setScreen(this.minecraft, new HudEditorScreen(this)), () -> this.onClose());

    public FeaturesScreen() {
        this(null);
    }

    public FeaturesScreen(Screen parent) {
        super(Component.literal(T.t("Menu de config")));
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

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) Env.leftHeld = true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0) Env.leftHeld = false;
        return super.mouseReleased(event);
    }
}
