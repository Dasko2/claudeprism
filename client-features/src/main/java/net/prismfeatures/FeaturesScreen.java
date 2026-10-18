package net.prismfeatures;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Map;

/** Menu en jeu (Maj droite) : un bouton par fonctionnalité, comme dans Lunar Client. */
public class FeaturesScreen extends Screen {
    private final Screen parent;

    public FeaturesScreen() {
        this(null);
    }

    public FeaturesScreen(Screen parent) {
        super(Text.literal("Menu de config"));
        this.parent = parent;
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    protected void init() {
        int w = 220;
        int h = 22;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - (Config.LABELS.size() * (h + 4)) / 2 + 8;
        for (Map.Entry<String, String> e : Config.LABELS.entrySet()) {
            final String key = e.getKey();
            ButtonWidget button = ButtonWidget.builder(label(key), b -> {
                Config.toggle(key);
                b.setMessage(label(key));
            }).dimensions(x, y, w, h).build();
            button.active = Config.allowed(key);
            this.addDrawableChild(button);
            y += h + 4;
        }
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fermer"), b -> this.close())
                .dimensions(x, y + 8, w, h).build());
    }

    private static Text label(String key) {
        String name = Config.LABELS.get(key);
        if (!Config.allowed(key)) return Text.literal(name + " : désactivé dans le launcher");
        return Text.literal(name + " : " + (Config.enabled(key) ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 78, 0xFFFFFFFF);
    }
}
