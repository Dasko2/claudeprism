package net.prismfeatures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Éléments d'affichage supplémentaires (CPS, touches, effets, combo, dégâts, boussole, viseur).
 * Ce fichier ne contient aucun type Minecraft : il est identique pour toutes les versions,
 * tout l'accès au jeu passe par {@link Env}.
 */
public final class Extras {
    private Extras() {}

    /** Élément d'affichage indépendant de la version ; adapté en Hud.Element par Hud. */
    public abstract static class Piece {
        public final String id;
        public final String label;
        public final String toggleKey;
        public final double defX;
        public final double defY;

        Piece(String id, String label, String toggleKey, double defX, double defY) {
            this.id = id;
            this.label = label;
            this.toggleKey = toggleKey;
            this.defX = defX;
            this.defY = defY;
        }

        public abstract int width(boolean preview);
        public abstract int height(boolean preview);
        public abstract void draw(Gfx g, int x, int y, boolean preview);
        public boolean hasContent() { return true; }
    }

    // ------------------------------------------------------------------ état mis à jour chaque tick
    private static final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private static boolean leftWas = false;
    private static boolean rightWas = false;
    private static int combo = 0;
    private static boolean hurtWas = false;
    private static float lastHealth = -1f;
    private static float lastLoss = 0f;
    private static long lastLossAt = 0L;

    private static long now() { return System.currentTimeMillis(); }

    private static int cps(ArrayDeque<Long> clicks) {
        long limit = now() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < limit) clicks.pollFirst();
        return clicks.size();
    }

    /** Appelé à chaque tick du client (en jeu). */
    public static void tick() {
        boolean left = Env.mouseDown(0);
        boolean right = Env.mouseDown(1);
        if (left && !leftWas) {
            leftClicks.addLast(now());
            if (Env.aimingAtEntity()) combo++;
        }
        if (right && !rightWas) rightClicks.addLast(now());
        leftWas = left;
        rightWas = right;

        boolean hurt = Env.justHurt();
        if (hurt && !hurtWas) combo = 0;
        hurtWas = hurt;

        float hp = Env.health();
        if (lastHealth >= 0 && hp < lastHealth - 0.01f) {
            lastLoss = lastHealth - hp;
            lastLossAt = now();
        }
        lastHealth = hp;
    }

    /** Remise à zéro quand on quitte le monde. */
    public static void reset() {
        leftClicks.clear();
        rightClicks.clear();
        combo = 0;
        lastHealth = -1f;
        lastLossAt = 0L;
    }

    // ------------------------------------------------------------------ éléments
    public static final Piece CPS = new Piece("cps", "CPS", Config.CPS, 0.0, 0.1) {
        private String text() { return "CPS : " + cps(leftClicks) + " | " + cps(rightClicks); }
        public int width(boolean p) { return Env.textWidth(text()); }
        public int height(boolean p) { return 10; }
        public void draw(Gfx g, int x, int y, boolean p) { g.text(text(), x, y, 0xFFFFFFFF); }
    };

    private static void keyBox(Gfx g, int x, int y, int w, int h, String label, boolean down) {
        g.fill(x, y, x + w, y + h, down ? 0xCCFFFFFF : 0x88000000);
        int tw = Env.textWidth(label);
        g.text(label, x + (w - tw) / 2, y + (h - 8) / 2, down ? 0xFF000000 : 0xFFFFFFFF);
    }

    public static final Piece KEYS = new Piece("keys", "Touches", Config.KEYS, 0.0, 0.75) {
        public int width(boolean p) { return 3 * 20 + 2 * 2; }
        public int height(boolean p) { return 3 * 20 + 2 * 2 + 4; }
        public void draw(Gfx g, int x, int y, boolean p) {
            keyBox(g, x + 22, y, 20, 20, "W", Env.keyDown("W"));
            keyBox(g, x, y + 22, 20, 20, "A", Env.keyDown("A"));
            keyBox(g, x + 22, y + 22, 20, 20, "S", Env.keyDown("S"));
            keyBox(g, x + 44, y + 22, 20, 20, "D", Env.keyDown("D"));
            keyBox(g, x, y + 44, 31, 20, "G " + cps(leftClicks), Env.mouseDown(0));
            keyBox(g, x + 33, y + 44, 31, 20, "D " + cps(rightClicks), Env.mouseDown(1));
        }
    };

    private static String roman(int n) {
        String[] r = { "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X" };
        return n >= 0 && n < r.length ? r[n] : " " + (n + 1);
    }

    public static final Piece EFFECTS = new Piece("effects", "Effets", Config.EFFECTS, 0.0, 0.3) {
        private List<String> lines(boolean preview) {
            List<String> out = new ArrayList<>();
            for (Env.Effect e : Env.effects()) {
                int s = e.ticks / 20;
                String time = e.ticks < 0 || s > 36000 ? "∞" : (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
                out.add(e.name + roman(e.amplifier) + "  " + time);
            }
            if (out.isEmpty() && preview) {
                out.add("Vitesse II  1:30");
                out.add("Force  0:45");
            }
            return out;
        }
        public int width(boolean p) {
            int w = 40;
            for (String l : lines(p)) w = Math.max(w, Env.textWidth(l));
            return w;
        }
        public int height(boolean p) { return Math.max(1, lines(p).size()) * 11; }
        public boolean hasContent() { return !lines(false).isEmpty(); }
        public void draw(Gfx g, int x, int y, boolean p) {
            for (String l : lines(p)) {
                g.text(l, x, y, 0xFFB8FFB8);
                y += 11;
            }
        }
    };

    public static final Piece COMBO = new Piece("combo", "Combo", Config.COMBO, 0.5, 0.55) {
        private String text() { return "Combo : " + combo; }
        public int width(boolean p) { return Env.textWidth(text()); }
        public int height(boolean p) { return 10; }
        public boolean hasContent() { return combo > 0; }
        public void draw(Gfx g, int x, int y, boolean p) { g.text(text(), x, y, combo >= 5 ? 0xFFFFAA00 : 0xFFFFFFFF); }
    };

    public static final Piece DAMAGE = new Piece("damage", "Dégâts reçus", Config.DAMAGE, 0.5, 0.62) {
        private String text() {
            if (lastLossAt == 0L) return "-3.5";
            return "-" + String.format("%.1f", lastLoss);
        }
        public int width(boolean p) { return Env.textWidth(text()); }
        public int height(boolean p) { return 10; }
        public boolean hasContent() { return lastLossAt != 0L && now() - lastLossAt < 2500; }
        public void draw(Gfx g, int x, int y, boolean p) { g.text(text(), x, y, 0xFFFF5555); }
    };

    private static final String[] DIR_NAMES = { "S", "SO", "O", "NO", "N", "NE", "E", "SE" };
    private static final int[] DIR_ANGLES = { 0, 45, 90, 135, 180, 225, 270, 315 };

    public static final Piece COMPASS = new Piece("compass", "Boussole", Config.COMPASS, 0.5, 0.02) {
        private static final int W = 150;
        public int width(boolean p) { return W; }
        public int height(boolean p) { return 22; }
        public void draw(Gfx g, int x, int y, boolean p) {
            float yaw = Env.yaw();
            g.fill(x, y, x + W, y + 22, 0x66000000);
            for (int i = 0; i < DIR_NAMES.length; i++) {
                float off = ((DIR_ANGLES[i] - yaw) % 360f + 540f) % 360f - 180f; // -180..180
                if (Math.abs(off) > 85f) continue;
                int cx = x + W / 2 + Math.round(off * (W / 2f) / 90f);
                String n = DIR_NAMES[i];
                g.text(n, cx - Env.textWidth(n) / 2, y + 3, n.equals("N") ? 0xFFFF5555 : 0xFFFFFFFF);
            }
            g.fill(x + W / 2, y + 14, x + W / 2 + 1, y + 21, 0xFFFFFFFF);
            String deg = Math.round(((yaw + 180f) % 360f + 360f) % 360f) + "°";
            g.text(deg, x + (W - Env.textWidth(deg)) / 2, y + 13, 0xFFBBBBBB);
        }
    };

    public static final Piece DAYS = new Piece("days", "Jours", Config.DAYS, 0.0, 0.2) {
        private String text() { return "Jour " + Env.day(); }
        public int width(boolean p) { return Env.textWidth(text()); }
        public int height(boolean p) { return 10; }
        public void draw(Gfx g, int x, int y, boolean p) { g.text(text(), x, y, 0xFFFFE08A); }
    };

    /** Tous les éléments, dans l'ordre de leur ajout à l'éditeur. */
    public static final List<Piece> PIECES = List.of(CPS, KEYS, EFFECTS, COMBO, DAMAGE, COMPASS, DAYS);

    // ------------------------------------------------------------------ viseur
    public static final String[] CROSS_STYLES = { "Normal", "Croix", "Point", "Cercle", "Croix + point" };
    public static final String[] CROSS_COLORS = { "Blanc", "Rouge", "Vert", "Bleu", "Jaune", "Violet" };
    private static final int[] CROSS_RGB = { 0xFFFFFFFF, 0xFFFF4444, 0xFF55FF55, 0xFF55AAFF, 0xFFFFDD33, 0xFFB080FF };

    public static boolean customCrosshair() { return Config.getInt("crosshairStyle", 0) != 0; }

    public static void drawCrosshair(Gfx g) {
        int style = Config.getInt("crosshairStyle", 0);
        if (style == 0) return;
        drawCrosshairAt(g, g.width() / 2, g.height() / 2, style, Config.getInt("crosshairColor", 0));
    }

    /** Dessine un viseur de style/couleur donnés (sert aussi à l'aperçu du menu). */
    public static void drawCrosshairAt(Gfx g, int cx, int cy, int style, int colorIndex) {
        int color = CROSS_RGB[Math.floorMod(colorIndex, CROSS_RGB.length)];
        if (style == 1 || style == 4) {
            g.fill(cx - 6, cy, cx - 2, cy + 1, color);
            g.fill(cx + 3, cy, cx + 7, cy + 1, color);
            g.fill(cx, cy - 6, cx + 1, cy - 2, color);
            g.fill(cx, cy + 3, cx + 1, cy + 7, color);
        }
        if (style == 2 || style == 4) g.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
        if (style == 3) {
            for (int dy = -4; dy <= 4; dy++) {
                int dx = (int) Math.round(Math.sqrt(Math.max(0, 16 - dy * dy)));
                g.fill(cx - dx - 1, cy + dy, cx - dx + 1, cy + dy + 1, color);
                g.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 1, color);
            }
            for (int dx = -2; dx <= 2; dx++) {
                int dy = (int) Math.round(Math.sqrt(Math.max(0, 16 - dx * dx)));
                g.fill(cx + dx, cy - dy - 1, cx + dx + 1, cy - dy + 1, color);
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 2, color);
            }
        }
    }
}
