package net.prismfeatures;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Menu de config en jeu : interrupteurs des fonctionnalités + touches modifiables. */
public class FeaturesScreen extends ScreenBase {
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
            final ButtonWidget[] ref = new ButtonWidget[1];
            ref[0] = Compat.button(label(key), () -> {
                Config.toggle(key);
                ref[0].setMessage(label(key));
            }, index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h);
            ref[0].active = Config.allowed(key);
            this.addDrawableChild(ref[0]);
            index++;
        }

        index = addKeyButton("nightVision", "Touche night vision", PrismFeatures.DEFAULT_KEY_NIGHT, index, left, right, top, w, h, gap);
        index = addKeyButton("zoom", "Touche zoom", PrismFeatures.DEFAULT_KEY_ZOOM, index, left, right, top, w, h, gap);
        index = addKeyButton("menu", "Touche du menu", PrismFeatures.DEFAULT_KEY_MENU, index, left, right, top, w, h, gap);

        final int below = top + rows * (h + gap) + 6;
        this.addDrawableChild(Compat.button(Text.literal("Placer les éléments à l'écran"),
                () -> this.client.setScreen(new HudEditorScreen(this)), this.width / 2 - 100, below, 200, h));
        this.addDrawableChild(Compat.button(Text.literal("Terminé"), () -> this.close(), this.width / 2 - 100, below + h + gap, 200, h));

        PrismFeatures.onBindingChanged = () -> refreshers.forEach(Runnable::run);
    }

    private int addKeyButton(String id, String name, int def, int index, int left, int right, int top, int w, int h, int gap) {
        final ButtonWidget[] ref = new ButtonWidget[1];
        ref[0] = Compat.button(keyLabel(id, name, def), () -> {
            PrismFeatures.listening = id;
            refreshers.forEach(Runnable::run);
        }, index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h);
        refreshers.add(() -> ref[0].setMessage(keyLabel(id, name, def)));
        this.addDrawableChild(ref[0]);
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
    protected void draw(Gfx g, int mouseX, int mouseY, float delta) {
        g.centered(this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        PrismFeatures.listening = null;
        PrismFeatures.onBindingChanged = null;
        if (this.client != null) this.client.setScreen(parent);
    }
}
