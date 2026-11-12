package net.prismfeatures;

import java.util.List;

public final class MenuUi {
    private static final int BG = 0xF0141420;
    private static final int SIDE = 0xFF0D0D17;
    private static final int ACCENT = 0xFF7C5CFF;
    private static final int ROW = 0xFF1C1C2B;
    private static final int ROW_HOVER = 0xFF26263A;
    private static final int OFF = 0xFF3A3A4C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9A9AB0;

    private static final int GREEN = 0xFF2E9E5B;
    private static final int GREEN_H = 0xFF3BB86C;
    private static final int RED = 0xFFB23B4A;
    private static final int RED_H = 0xFFCC4A5A;

    private static final String[] TABS = { T.t("MODS"), T.t("VISEUR"), T.t("TOUCHES") };
    private static final String[] FILTERS = { T.t("TOUT"), T.t("JEU"), T.t("AFFICHAGE") };

    private static final String[][] MODS = {
        { Config.NIGHT_VISION, T.t("Voir dans le noir"), "0" },
        { Config.ZOOM, T.t("Maintenir la touche pour zoomer"), "0" },
        { Config.SHULKER, T.t("Contenu au survol d'un shulker"), "0" },
        { Config.HUD, T.t("FPS, position, ping, heure"), "1" },
        { Config.ARMOR, T.t("Durabilité de l'armure"), "1" },
        { Config.MAIN_HAND, T.t("Objet tenu et quantité"), "1" },
        { Config.OFF_HAND, T.t("Seconde main"), "1" },
        { Config.DEATH, T.t("Coordonnées de la mort"), "1" },
        { Config.CPS, T.t("Clics par seconde"), "1" },
        { Config.KEYS, T.t("W A S D et souris"), "1" },
        { Config.EFFECTS, T.t("Effets de potion actifs"), "1" },
        { Config.COMBO, T.t("Coups enchaînés"), "1" },
        { Config.DAMAGE, T.t("Dernier coup reçu"), "1" },
        { Config.COMPASS, T.t("Barre de direction"), "1" },
        { Config.DAYS, T.t("Numéro du jour du monde"), "1" }
    };

    private int tab = 0;
    private int filter = 0;
    private boolean wasDown = true;
    private final Runnable openEditor;
    private final Runnable close;

    private Gfx g;
    private int mx, my;
    private boolean click;

    public MenuUi(Runnable openEditor, Runnable close) {
        Env.clickPending = false;
        this.openEditor = openEditor;
        this.close = close;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void center(String s, int x, int y, int w, int color) {
        g.text(s, x + (w - Env.textWidth(s)) / 2, y, color);
    }

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
        g.text(allowed ? desc : T.t("Désactivé dans le launcher"), x + 8, y + 15, MUTED);
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
        String value = listening ? T.t("Appuyez sur une touche...") : PrismFeatures.keyName(Config.key(id, def));
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

    private boolean small(String label, int x, int y, int w, int h, int c, int ch) {
        boolean hover = in(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? ch : c);
        center(label, x, y + (h - 8) / 2, w, TEXT);
        return hover && click;
    }

    private void card(String[] m, int x, int y, int w, int h) {
        String key = m[0];
        boolean allowed = Config.allowed(key);
        boolean on = Config.enabled(key);
        boolean hud = "1".equals(m[2]);
        boolean hover = in(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? ROW_HOVER : ROW);
        g.fill(x, y, x + 2, y + h, allowed && on ? GREEN : allowed ? RED : OFF);
        String name = Config.LABELS.get(key);
        int is = 20;
        g.fill(x + 7, y + 5, x + 7 + is, y + 5 + is, hud ? 0xFF2B2450 : 0xFF1F3A33);
        center(name.substring(0, 1).toUpperCase(), x + 7, y + 5 + (is - 8) / 2, is, hud ? ACCENT : 0xFF5FD39A);
        g.text(name, x + 34, y + 6, allowed ? TEXT : MUTED);
        g.text(allowed ? m[1] : T.t("Désactivé dans le launcher"), x + 34, y + 16, MUTED);
        int by = y + h - 14;
        int bh = 11;
        int bx = x + 7;
        int bw = w - 14;
        if (hud && allowed) {
            int pw = 52;
            if (small(T.t("PLACER"), bx, by, pw, bh, OFF, 0xFF4B4B60)) openEditor.run();
            bx += pw + 4;
            bw -= pw + 4;
        }
        if (!allowed) {
            g.fill(bx, by, bx + bw, by + bh, 0xFF2A2A36);
            center(T.t("VERROUILLÉ"), bx, by + 2, bw, MUTED);
        } else if (small(on ? T.t("ACTIVÉ") : T.t("DÉSACTIVÉ"), bx, by, bw, bh, on ? GREEN : RED, on ? GREEN_H : RED_H)) {
            Config.toggle(key);
        }
    }

    private void mods(int x, int y, int w, int bottom) {
        int cx = x;
        for (int i = 0; i < FILTERS.length; i++) {
            int fw = Env.textWidth(FILTERS[i]) + 18;
            boolean sel = filter == i;
            boolean hover = in(mx, my, cx, y, fw, 14);
            g.fill(cx, y, cx + fw, y + 14, sel ? ACCENT : hover ? ROW_HOVER : ROW);
            center(FILTERS[i], cx, y + 3, fw, sel ? TEXT : MUTED);
            if (hover && click) filter = i;
            cx += fw + 4;
        }
        int total = 0;
        int enabled = 0;
        for (String[] m : MODS) {
            if (Config.allowed(m[0])) {
                total++;
                if (Config.enabled(m[0])) enabled++;
            }
        }
        String count = enabled + "/" + total + " " + T.t("activés");
        g.text(count, x + w - Env.textWidth(count), y + 3, MUTED);

        List<String[]> shown = new java.util.ArrayList<>();
        for (String[] m : MODS) {
            if (filter == 0 || (filter == 1 && "0".equals(m[2])) || (filter == 2 && "1".equals(m[2]))) shown.add(m);
        }
        int cols = w >= 360 ? 3 : w >= 240 ? 2 : 1;
        int gap = 5;
        int cw = (w - (cols - 1) * gap) / cols;
        int top = y + 20;
        int rows = (shown.size() + cols - 1) / cols;
        int ch = Math.max(30, Math.min(42, (bottom - top - (rows - 1) * gap) / Math.max(1, rows)));
        for (int i = 0; i < shown.size(); i++) {
            card(shown.get(i), x + (i % cols) * (cw + gap), top + (i / cols) * (ch + gap), cw, ch);
        }
    }

    public void draw(Gfx gfx, int mouseX, int mouseY) {
        this.g = gfx;
        this.mx = mouseX;
        this.my = mouseY;
        boolean down = Env.mouseDown(0);
        boolean latched = Env.consumeClick();
        if (latched) {
            this.mx = (int) Env.clickX;
            this.my = (int) Env.clickY;
        }
        this.click = (down && !wasDown) | latched;
        wasDown = down;

        int sw = gfx.width();
        int sh = gfx.height();
        int W = Math.min(sw - 16, 560);
        int H = Math.min(sh - 16, 310);
        int X = (sw - W) / 2;
        int Y = (sh - H) / 2;

        g.fill(X - 1, Y - 1, X + W + 1, Y + H + 1, 0xFF05050A);
        g.fill(X, Y, X + W, Y + H, BG);
        g.fill(X, Y, X + W, Y + 26, SIDE);
        g.fill(X, Y + 26, X + W, Y + 27, 0xFF2A2A3C);
        g.fill(X, Y, X + W, Y + 2, ACCENT);

        g.text("PRISMLAUNCHER", X + 10, Y + 9, ACCENT);
        int tx = X + 10 + Env.textWidth("PRISMLAUNCHER") + 20;
        for (int i = 0; i < TABS.length; i++) {
            int tw = Env.textWidth(TABS[i]) + 20;
            boolean sel = i == tab;
            boolean hover = in(mx, my, tx, Y + 4, tw, 22);
            g.text(TABS[i], tx + 10, Y + 10, sel ? TEXT : hover ? 0xFFCFCFE6 : MUTED);
            if (sel) g.fill(tx, Y + 25, tx + tw, Y + 27, ACCENT);
            if (hover && click) tab = i;
            tx += tw + 2;
        }
        if (small("X", X + W - 24, Y + 6, 16, 14, ROW, RED)) close.run();

        int cx = X + 14;
        int cw = W - 28;
        int top = Y + 36;
        int bottom = Y + H - 34;

        switch (tab) {
            case 0 -> mods(cx, top, cw, bottom);
            case 1 -> {
                cycleRow(T.t("Style"), Extras.CROSS_STYLES, "crosshairStyle", cx, top, cw);
                cycleRow(T.t("Couleur"), Extras.CROSS_COLORS, "crosshairColor", cx, top + 30, cw);
                int py = top + 66;
                int ph = Math.max(40, Math.min(90, bottom - py));
                g.fill(cx, py, cx + cw, py + ph, ROW);
                g.text(T.t("Aperçu"), cx + 8, py + 6, MUTED);
                int style = Config.getInt("crosshairStyle", 0);
                Extras.drawCrosshairAt(g, cx + cw / 2, py + ph / 2 + 4, style == 0 ? 1 : style, Config.getInt("crosshairColor", 0));
                if (style == 0) g.text(T.t("Viseur de base du jeu"), cx + 8, py + ph - 12, MUTED);
            }
            default -> {
                keyRow("nightVision", T.t("Night vision"), PrismFeatures.DEFAULT_KEY_NIGHT, cx, top, cw);
                keyRow("zoom", T.t("Zoom"), PrismFeatures.DEFAULT_KEY_ZOOM, cx, top + 30, cw);
                keyRow("menu", T.t("Ouvrir ce menu"), PrismFeatures.DEFAULT_KEY_MENU, cx, top + 60, cw);
                g.text(T.t("Cliquez sur une touche puis appuyez sur la nouvelle."), cx, top + 96, MUTED);
            }
        }

        g.fill(X, Y + H - 28, X + W, Y + H - 27, 0xFF2A2A3C);
        if (button(T.t("Placer à l'écran"), X + 14, Y + H - 23, 110, 18, false)) openEditor.run();
        if (button(T.t("Fermer"), X + W - 84, Y + H - 23, 70, 18, true)) close.run();
    }
}
