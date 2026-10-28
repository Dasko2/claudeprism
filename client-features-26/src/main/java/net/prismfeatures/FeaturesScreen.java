package net.prismfeatures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

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
        super(Component.literal("Menu de config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshers.clear();
        final int h = 20;
        final int gap = 4;
        final int total = Config.LABELS.size() + 5;
        final int cols = total * (h + gap) / 2 > this.height - 90 ? 3 : 2;
        final int w = cols == 3 ? Math.min(150, (this.width - 20) / 3 - gap) : 200;
        final int rows = (total + cols - 1) / cols;
        final int x0 = this.width / 2 - (cols * w + (cols - 1) * gap) / 2;
        final int top = Math.max(30, this.height / 2 - (rows * (h + gap)) / 2 - 6);
        int index = 0;

        for (Map.Entry<String, String> e : Config.LABELS.entrySet()) {
            final String key = e.getKey();
            Button b = Button.builder(label(key), btn -> {
                Config.toggle(key);
                btn.setMessage(label(key));
            }).bounds(x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h).build();
            b.active = Config.allowed(key);
            this.addRenderableWidget(b);
            index++;
        }

        index = addKeyButton("nightVision", "Night vision", PrismFeatures.DEFAULT_KEY_NIGHT, index, cols, x0, top, w, h, gap);
        index = addKeyButton("zoom", "Zoom", PrismFeatures.DEFAULT_KEY_ZOOM, index, cols, x0, top, w, h, gap);
        index = addKeyButton("menu", "Menu", PrismFeatures.DEFAULT_KEY_MENU, index, cols, x0, top, w, h, gap);

        this.addRenderableWidget(Button.builder(styleLabel(), btn -> {
            Config.setInt("crosshairStyle", (Config.getInt("crosshairStyle", 0) + 1) % Extras.CROSS_STYLES.length);
            btn.setMessage(styleLabel());
        }).bounds(x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h).build());
        index++;
        this.addRenderableWidget(Button.builder(colorLabel(), btn -> {
            Config.setInt("crosshairColor", (Config.getInt("crosshairColor", 0) + 1) % Extras.CROSS_COLORS.length);
            btn.setMessage(colorLabel());
        }).bounds(x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h).build());

        final int below = top + rows * (h + gap) + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Placer les éléments à l'écran"),
                btn -> Compat.setScreen(this.minecraft, new HudEditorScreen(this))).bounds(this.width / 2 - 100, below, 200, h).build());
        this.addRenderableWidget(Button.builder(Component.literal("Terminé"), btn -> this.onClose())
                .bounds(this.width / 2 - 100, below + h + gap, 200, h).build());

        PrismFeatures.onBindingChanged = () -> refreshers.forEach(Runnable::run);
    }

    private static Component styleLabel() {
        return Component.literal("Viseur : " + Extras.CROSS_STYLES[Math.floorMod(Config.getInt("crosshairStyle", 0), Extras.CROSS_STYLES.length)]);
    }

    private static Component colorLabel() {
        return Component.literal("Couleur viseur : " + Extras.CROSS_COLORS[Math.floorMod(Config.getInt("crosshairColor", 0), Extras.CROSS_COLORS.length)]);
    }

    private int addKeyButton(String id, String name, int def, int index, int cols, int x0, int top, int w, int h, int gap) {
        Button b = Button.builder(keyLabel(id, name, def), btn -> {
            PrismFeatures.listening = id;
            refreshers.forEach(Runnable::run);
        }).bounds(x0 + (index % cols) * (w + gap), top + (index / cols) * (h + gap), w, h).build();
        refreshers.add(() -> b.setMessage(keyLabel(id, name, def)));
        this.addRenderableWidget(b);
        return index + 1;
    }

    private static Component keyLabel(String id, String name, int def) {
        if (id.equals(PrismFeatures.listening)) return Component.literal("Touche " + name + " : ...");
        return Component.literal("Touche " + name + " : [" + PrismFeatures.keyName(Config.key(id, def)) + "]");
    }

    private static Component label(String key) {
        String name = Config.LABELS.get(key);
        if (!Config.allowed(key)) return Component.literal(name + " : désactivé dans le launcher");
        return Component.literal(name + " : " + (Config.enabled(key) ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        PrismFeatures.listening = null;
        PrismFeatures.onBindingChanged = null;
        if (this.minecraft != null) Compat.setScreen(this.minecraft, parent);
    }
}
