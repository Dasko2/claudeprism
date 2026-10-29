package net.prismfeatures;

import java.util.List;

/**
 * Menu de configuration façon launcher : barre latérale, onglets, interrupteurs, sélecteurs.
 * Aucun type Minecraft : tout passe par Gfx et Env, donc identique pour toutes les versions.
 * Les clics sont lus par sondage de la souris (comme l'éditeur de placement).
 */
public final class MenuUi {
    private static final int BG = 0xF0141420;
    private static final int SIDE = 0xFF0D0D17;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ROW = 0xFF1C1C2B;
    private static final int ROW_HOVER = 0xFF26263A;
    private static final int OFF = 0xFF3A3A4C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9A9AB0;

    private static final String[] TABS = { "Général", "Affichage", "Viseur", "Touches" };
    private static final String[] TAB_SUB = {
        "Fonctions de jeu", "Éléments à l'écran", "Personnalisation du viseur", "Raccourcis clavier" };

    private int tab = 0;
    private boolean wasDown = true; // évite un clic fantôme à l'ouverture
    private final Runnable openEditor;
    private final Runnable close;

    // état d'une image
    private Gfx g;
    private int mx, my;
    private boolean click;

    public MenuUi(Runnable openEditor, Runnable close) {
        this.openEditor = openEditor;
        this.close = close;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void center(String s, int x, int y, int w, int color) {
        g.text(s, x + (w - Env.textWidth(s)) / 2, y, color);
    }

    /** Bouton rectangulaire ; renvoie vrai au clic. */
    private boolean button(String label, int x, int y, int w, int h, boolean primary) {
        boolean hover = in(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, primary ? (hover ? 0xFF8F74FF : ACCENT) : (hover ? ROW_HOVER : ROW));
        center(label, x, y + (h - 8) / 2, w, TEXT);
        return hover && click;
    }

    private void toggleRow(String key, String desc, int x, int y, int w) {
        final int h = 26;
        boolean allowed = Config.allowed(key);
        boolean on = Config.enabled(key);
        boolean hover = allowed && in(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? ROW_HOVER : ROW);
        g.text(Config.LABELS.get(key), x + 8, y + 4, allowed ? TEXT : MUTED);
        g.text(allowed ? desc : "Désactivé dans le launcher", x + 8, y + 15, MUTED);
        int sx = x + w - 34;
        int sy = y + 7;
        g.fill(sx, sy, sx + 26, sy + 12, !allowed ? 0xFF2A2A36 : on ? ACCENT : OFF);
        int kx = on ? sx + 15 : sx + 1;
        g.fill(kx, sy + 1, kx + 10, sy + 11, allowed ? 0xFFFFFFFF : 0xFF777788);
        if (hover && click) Config.toggle(key);
    }

    private void keyRow(String id, String label, int def, int x, int y, int w) {
        final int h = 26;
        boolean hover = in(mx, my, x, y, w, h);
        boolean listening = id.equals(PrismFeatures.listening);
        g.fill(x, y, x + w, y + h, hover ? ROW_HOVER : ROW);
        g.text(label, x + 8, y + 9, TEXT);
        String value = listening ? "Appuyez sur une touche..." : PrismFeatures.keyName(Config.key(id, def));
        int bw = Math.max(70, Env.textWidth(value) + 16);
        int bx = x + w - bw - 6;
        g.fill(bx, y + 4, bx + bw, y + h - 4, listening ? ACCENT : OFF);
        center(value, bx, y + 9, bw, TEXT);
        if (hover && click) PrismFeatures.listening = id;
    }

    private void cycleRow(String label, String[] values, String cfgKey, int x, int y, int w) {
        final int h = 26;
        int cur = Math.floorMod(Config.getInt(cfgKey, 0), values.length);
        g.fill(x, y, x + w, y + h, ROW);
        g.text(label, x + 8, y + 9, TEXT);
        int bw = 120;
        int bx = x + w - bw - 6;
        boolean hl = in(mx, my, bx, y + 3, 20, h - 6);
        boolean hr = in(mx, my, bx + bw - 20, y + 3, 20, h - 6);
        g.fill(bx, y + 3, bx + 20, y + h - 3, hl ? ACCENT : OFF);
        g.fill(bx + bw - 20, y + 3, bx + bw, y + h - 3, hr ? ACCENT : OFF);
        center("<", bx, y + 9, 20, TEXT);
        center(">", bx + bw - 20, y + 9, 20, TEXT);
        center(values[cur], bx + 20, y + 9, bw - 40, TEXT);
        if (click && hl) Config.setInt(cfgKey, (cur + values.length - 1) % values.length);
        if (click && hr) Config.setInt(cfgKey, (cur + 1) % values.length);
    }

    private void toggles(List<String[]> rows, int x, int y, int w, int cols) {
        int cw = (w - (cols - 1) * 6) / cols;
        for (int i = 0; i < rows.size(); i++) {
            toggleRow(rows.get(i)[0], rows.get(i)[1], x + (i % cols) * (cw + 6), y + (i / cols) * 30, cw);
        }
    }

    /** Dessine tout le menu et traite les clics. */
    public void draw(Gfx gfx, int mouseX, int mouseY) {
        this.g = gfx;
        this.mx = mouseX;
        this.my = mouseY;
        boolean down = Env.mouseDown(0);
        this.click = down && !wasDown;
        wasDown = down;

        int sw = gfx.width();
        int sh = gfx.height();
        int W = Math.min(sw - 16, 520);
        int H = Math.min(sh - 16, 290);
        int X = (sw - W) / 2;
        int Y = (sh - H) / 2;
        final int sideW = 108;

        // ombre + panneau
        g.fill(X - 1, Y - 1, X + W + 1, Y + H + 1, 0xFF05050A);
        g.fill(X, Y, X + W, Y + H, BG);
        g.fill(X, Y, X + sideW, Y + H, SIDE);
        g.fill(X, Y, X + W, Y + 2, ACCENT);

        // barre latérale
        g.text("PRISMLAUNCHER", X + 10, Y + 10, ACCENT);
        g.text("Menu de config", X + 10, Y + 21, MUTED);
        for (int i = 0; i < TABS.length; i++) {
            int ty = Y + 42 + i * 24;
            boolean sel = i == tab;
            boolean hover = in(mx, my, X + 6, ty, sideW - 12, 20);
            g.fill(X + 6, ty, X + sideW - 6, ty + 20, sel ? 0xFF241E44 : hover ? ROW_HOVER : SIDE);
            if (sel) g.fill(X + 6, ty, X + 8, ty + 20, ACCENT);
            g.text(TABS[i], X + 16, ty + 6, sel ? TEXT : MUTED);
            if (hover && click) tab = i;
        }
        if (button("Placer à l'écran", X + 6, Y + H - 50, sideW - 12, 20, false)) openEditor.run();
        if (button("Fermer", X + 6, Y + H - 26, sideW - 12, 20, true)) close.run();

        // contenu
        int cx = X + sideW + 14;
        int cw = W - sideW - 28;
        g.text(TABS[tab], cx, Y + 12, TEXT);
        g.text(TAB_SUB[tab], cx, Y + 23, MUTED);
        g.fill(cx, Y + 34, cx + cw, Y + 35, 0xFF2A2A3C);
        int top = Y + 42;

        switch (tab) {
            case 0 -> toggles(List.of(
                new String[] { Config.NIGHT_VISION, "Voir dans le noir" },
                new String[] { Config.ZOOM, "Maintenir la touche pour zoomer" },
                new String[] { Config.SHULKER, "Contenu au survol d'un shulker" }), cx, top, cw, 1);
            case 1 -> toggles(List.of(
                new String[] { Config.HUD, "FPS, position, ping, heure" },
                new String[] { Config.ARMOR, "Durabilité de l'armure" },
                new String[] { Config.MAIN_HAND, "Objet tenu et quantité" },
                new String[] { Config.OFF_HAND, "Seconde main" },
                new String[] { Config.DEATH, "Coordonnées de la mort" },
                new String[] { Config.CPS, "Clics par seconde" },
                new String[] { Config.KEYS, "W A S D et souris" },
                new String[] { Config.EFFECTS, "Effets de potion actifs" },
                new String[] { Config.COMBO, "Coups enchaînés" },
                new String[] { Config.DAMAGE, "Dernier coup reçu" },
                new String[] { Config.COMPASS, "Barre de direction" },
                new String[] { Config.DAYS, "Numéro du jour du monde" }), cx, top, cw, cw >= 300 ? 2 : 1);
            case 2 -> {
                cycleRow("Style", Extras.CROSS_STYLES, "crosshairStyle", cx, top, cw);
                cycleRow("Couleur", Extras.CROSS_COLORS, "crosshairColor", cx, top + 30, cw);
                int py = top + 66;
                int ph = Math.min(80, Y + H - py - 10);
                g.fill(cx, py, cx + cw, py + ph, ROW);
                g.text("Aperçu", cx + 8, py + 6, MUTED);
                int style = Config.getInt("crosshairStyle", 0);
                Extras.drawCrosshairAt(g, cx + cw / 2, py + ph / 2 + 4, style == 0 ? 1 : style, Config.getInt("crosshairColor", 0));
                if (style == 0) g.text("Viseur de base du jeu", cx + 8, py + ph - 12, MUTED);
            }
            default -> {
                keyRow("nightVision", "Night vision", PrismFeatures.DEFAULT_KEY_NIGHT, cx, top, cw);
                keyRow("zoom", "Zoom", PrismFeatures.DEFAULT_KEY_ZOOM, cx, top + 30, cw);
                keyRow("menu", "Ouvrir ce menu", PrismFeatures.DEFAULT_KEY_MENU, cx, top + 60, cw);
                g.text("Cliquez sur une touche puis appuyez sur la nouvelle.", cx, top + 96, MUTED);
            }
        }
    }
}
