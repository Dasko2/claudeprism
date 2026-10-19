package net.prismfeatures;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Menu de config en jeu : interrupteurs des fonctionnalités + touches modifiables. */
public class FeaturesScreen extends Screen {
    private final Screen parent;
    private final List<Runnable> refreshers = new ArrayList<>();

    public FeaturesScreen() {
        this(null);
    }

    public FeaturesScreen(Screen parent) {
        super(Text.literal("Menu de config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshers.clear();
        final int w = 200;
        final int h = 20;
        final int gap = 4;
        final int total = Config.LABELS.size() + 3;
        final int rows = (total + 1) / 2;
        final int left = this.width / 2 - w - gap / 2;
        final int right = this.width / 2 + gap / 2;
        final int top = Math.max(34, this.height / 2 - (rows * (h + gap)) / 2 - 6);
        int index = 0;

        for (Map.Entry<String, String> e : Config.LABELS.entrySet()) {
            final String key = e.getKey();
            ButtonWidget b = ButtonWidget.builder(label(key), btn -> {
                Config.toggle(key);
                btn.setMessage(label(key));
            }).dimensions(index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h).build();
            b.active = Config.allowed(key);
            this.addDrawableChild(b);
            index++;
        }

        index = addKeyButton("nightVision", "Touche night vision", PrismFeatures.DEFAULT_KEY_NIGHT, index, left, right, top, w, h, gap);
        index = addKeyButton("zoom", "Touche zoom", PrismFeatures.DEFAULT_KEY_ZOOM, index, left, right, top, w, h, gap);
        index = addKeyButton("menu", "Touche du menu", PrismFeatures.DEFAULT_KEY_MENU, index, left, right, top, w, h, gap);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Terminé"), btn -> this.close())
                .dimensions(this.width / 2 - 100, top + rows * (h + gap) + 8, 200, h).build());

        PrismFeatures.onBindingChanged = () -> refreshers.forEach(Runnable::run);
    }

    private int addKeyButton(String id, String name, int def, int index, int left, int right, int top, int w, int h, int gap) {
        ButtonWidget b = ButtonWidget.builder(keyLabel(id, name, def), btn -> {
            PrismFeatures.listening = id;
            refreshers.forEach(Runnable::run);
        }).dimensions(index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h).build();
        refreshers.add(() -> b.setMessage(keyLabel(id, name, def)));
        this.addDrawableChild(b);
        return index + 1;
    }

    private static Text keyLabel(String id, String name, int def) {
        if (id.equals(PrismFeatures.listening)) return Text.literal(name + " : appuyez sur une touche...");
        return Text.literal(name + " : [" + PrismFeatures.keyName(Config.key(id, def)) + "]");
    }

    private static Text label(String key) {
        String name = Config.LABELS.get(key);
        if (!Config.allowed(key)) return Text.literal(name + " : désactivé dans le launcher");
        return Text.literal(name + " : " + (Config.enabled(key) ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        PrismFeatures.listening = null;
        PrismFeatures.onBindingChanged = null;
        if (this.client != null) this.client.setScreen(parent);
    }
}
