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
        final int h = 20;
        final int gap = 4;
        final int total = Config.LABELS.size() + 6; // + 3 touches + 2 réglages du viseur
        final int cols = total * (h + gap) / 2 > this.height - 90 ? 3 : 2;
        final int w = cols == 3 ? Math.min(150, (this.width - 20) / 3 - gap) : 200;
        final int rows = (total + cols - 1) / cols;
        final int x0 = this.width / 2 - (cols * w + (cols - 1) * gap) / 2;
        final int top = Math.max(30, this.height / 2 - (rows * (h + gap)) / 2 - 6);
        int index = 0;

        for (Map.Entry<String, String> e : Config.LABELS.entrySet()) {
            final String key = e.getKey();
            final ButtonWidget[] ref = new ButtonWidget[1];
            ref[0] = Compat.button(label(key), () -> {
                Config.toggle(key);
                ref[0].setMessage(label(key));
            }, x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h);
            ref[0].active = Config.allowed(key);
            this.addDrawableChild(ref[0]);
            index++;
        }

        index = addKeyButton("nightVision", "Night vision", PrismFeatures.DEFAULT_KEY_NIGHT, index, cols, x0, top, w, h, gap);
        index = addKeyButton("zoom", "Zoom", PrismFeatures.DEFAULT_KEY_ZOOM, index, cols, x0, top, w, h, gap);
        index = addKeyButton("freelook", "Freelook", PrismFeatures.DEFAULT_KEY_FREELOOK, index, cols, x0, top, w, h, gap);
        index = addKeyButton("menu", "Menu", PrismFeatures.DEFAULT_KEY_MENU, index, cols, x0, top, w, h, gap);

        final ButtonWidget[] style = new ButtonWidget[1];
        style[0] = Compat.button(styleLabel(), () -> {
            Config.setInt("crosshairStyle", (Config.getInt("crosshairStyle", 0) + 1) % Extras.CROSS_STYLES.length);
            style[0].setMessage(styleLabel());
        }, x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h);
        this.addDrawableChild(style[0]);
        index++;
        final ButtonWidget[] color = new ButtonWidget[1];
        color[0] = Compat.button(colorLabel(), () -> {
            Config.setInt("crosshairColor", (Config.getInt("crosshairColor", 0) + 1) % Extras.CROSS_COLORS.length);
            color[0].setMessage(colorLabel());
        }, x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h);
        this.addDrawableChild(color[0]);

        final int below = top + rows * (h + gap) + 6;
        this.addDrawableChild(Compat.button(Text.literal("Placer les éléments à l'écran"),
                () -> this.client.setScreen(new HudEditorScreen(this)), this.width / 2 - 100, below, 200, h));
        this.addDrawableChild(Compat.button(Text.literal("Terminé"), () -> this.close(), this.width / 2 - 100, below + h + gap, 200, h));

        PrismFeatures.onBindingChanged = () -> refreshers.forEach(Runnable::run);
    }

    private static Text styleLabel() {
        return Text.literal("Viseur : " + Extras.CROSS_STYLES[Math.floorMod(Config.getInt("crosshairStyle", 0), Extras.CROSS_STYLES.length)]);
    }

    private static Text colorLabel() {
        return Text.literal("Couleur viseur : " + Extras.CROSS_COLORS[Math.floorMod(Config.getInt("crosshairColor", 0), Extras.CROSS_COLORS.length)]);
    }

    private int addKeyButton(String id, String name, int def, int index, int cols, int x0, int top, int w, int h, int gap) {
        final ButtonWidget[] ref = new ButtonWidget[1];
        ref[0] = Compat.button(keyLabel(id, name, def), () -> {
            PrismFeatures.listening = id;
            refreshers.forEach(Runnable::run);
        }, x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h);
        refreshers.add(() -> ref[0].setMessage(keyLabel(id, name, def)));
        this.addDrawableChild(ref[0]);
        return index + 1;
    }

    private static Text keyLabel(String id, String name, int def) {
        if (id.equals(PrismFeatures.listening)) return Text.literal("Touche " + name + " : ...");
        return Text.literal("Touche " + name + " : [" + PrismFeatures.keyName(Config.key(id, def)) + "]");
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
