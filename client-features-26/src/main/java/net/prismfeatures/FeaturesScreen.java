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
        final int w = 200;
        final int h = 20;
        final int gap = 4;
        final int total = Config.LABELS.size() + 4;
        final int rows = (total + 1) / 2;
        final int left = this.width / 2 - w - gap / 2;
        final int right = this.width / 2 + gap / 2;
        final int top = Math.max(34, this.height / 2 - (rows * (h + gap)) / 2 - 6);
        int index = 0;

        for (Map.Entry<String, String> e : Config.LABELS.entrySet()) {
            final String key = e.getKey();
            Button b = Button.builder(label(key), btn -> {
                Config.toggle(key);
                btn.setMessage(label(key));
            }).bounds(index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h).build();
            b.active = Config.allowed(key);
            this.addRenderableWidget(b);
            index++;
        }

        index = addKeyButton("nightVision", "Touche night vision", PrismFeatures.DEFAULT_KEY_NIGHT, index, left, right, top, w, h, gap);
        index = addKeyButton("zoom", "Touche zoom", PrismFeatures.DEFAULT_KEY_ZOOM, index, left, right, top, w, h, gap);
        index = addKeyButton("shulkerOpen", "Touche ouvrir shulker (solo)", PrismFeatures.DEFAULT_KEY_SHULKER, index, left, right, top, w, h, gap);
        index = addKeyButton("menu", "Touche du menu", PrismFeatures.DEFAULT_KEY_MENU, index, left, right, top, w, h, gap);

        final int below = top + rows * (h + gap) + 6;
        this.addRenderableWidget(Button.builder(Component.literal("Placer les éléments à l'écran"),
                btn -> Compat.setScreen(this.minecraft, new HudEditorScreen(this))).bounds(this.width / 2 - 100, below, 200, h).build());
        this.addRenderableWidget(Button.builder(Component.literal("Terminé"), btn -> this.onClose())
                .bounds(this.width / 2 - 100, below + h + gap, 200, h).build());

        PrismFeatures.onBindingChanged = () -> refreshers.forEach(Runnable::run);
    }

    private int addKeyButton(String id, String name, int def, int index, int left, int right, int top, int w, int h, int gap) {
        Button b = Button.builder(keyLabel(id, name, def), btn -> {
            PrismFeatures.listening = id;
            refreshers.forEach(Runnable::run);
        }).bounds(index % 2 == 0 ? left : right, top + (index / 2) * (h + gap), w, h).build();
        refreshers.add(() -> b.setMessage(keyLabel(id, name, def)));
        this.addRenderableWidget(b);
        return index + 1;
    }

    private static Component keyLabel(String id, String name, int def) {
        if (id.equals(PrismFeatures.listening)) return Component.literal(name + " : appuyez sur une touche...");
        return Component.literal(name + " : [" + PrismFeatures.keyName(Config.key(id, def)) + "]");
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
