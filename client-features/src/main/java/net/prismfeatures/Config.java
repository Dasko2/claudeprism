package net.prismfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {
    public static final String NIGHT_VISION = "nightVision";
    public static final String SHULKER = "shulkerPreview";
    public static final String ZOOM = "zoom";
    public static final String HUD = "hud";
    public static final String ARMOR = "armor";
    public static final String MAIN_HAND = "mainHand";
    public static final String OFF_HAND = "offHand";
    public static final String DEATH = "deathPoint";
    public static final String CPS = "cps";
    public static final String KEYS = "keys";
    public static final String EFFECTS = "effects";
    public static final String COMBO = "combo";
    public static final String DAMAGE = "damage";
    public static final String COMPASS = "compass";
    public static final String DAYS = "days";

    private static final java.util.Set<String> OFF_BY_DEFAULT = java.util.Set.of(CPS, KEYS, EFFECTS, COMBO, DAMAGE, COMPASS, DAYS);

    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put(NIGHT_VISION, T.t("Night vision"));
        LABELS.put(SHULKER, T.t("Vision shulkers"));
        LABELS.put(ZOOM, T.t("Zoom"));
        LABELS.put(HUD, T.t("Infos (FPS, XYZ...)"));
        LABELS.put(ARMOR, T.t("Armure"));
        LABELS.put(MAIN_HAND, T.t("Objet en main"));
        LABELS.put(OFF_HAND, T.t("Seconde main"));
        LABELS.put(DEATH, T.t("Point de mort"));
        LABELS.put(CPS, T.t("CPS"));
        LABELS.put(KEYS, T.t("Touches affichées"));
        LABELS.put(EFFECTS, T.t("Effets de potion"));
        LABELS.put(COMBO, T.t("Combo"));
        LABELS.put(DAMAGE, T.t("Dégâts reçus"));
        LABELS.put(COMPASS, T.t("Boussole"));
        LABELS.put(DAYS, T.t("Compteur de jours"));
    }

    private static JsonObject launcher = new JsonObject();
    private static JsonObject user = new JsonObject();

    public static void writeStatus(String server) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("server", server);
            Files.writeString(dir().resolve("prismfeatures-status.json"), new Gson().toJson(o));
        } catch (Exception ignored) {
        }
    }

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void load() {
        launcher = read(dir().resolve("prismfeatures.json"));
        user = read(dir().resolve("prismfeatures-user.json"));
    }

    private static JsonObject read(Path file) {
        try {
            if (Files.exists(file)) {
                JsonObject o = new Gson().fromJson(Files.readString(file), JsonObject.class);
                if (o != null) return o;
            }
        } catch (Exception e) {
            PrismFeatures.LOGGER.warn("Config illisible : {}", file, e);
        }
        return new JsonObject();
    }

    private static boolean get(JsonObject o, String key) {
        return !o.has(key) || o.get(key).getAsBoolean();
    }

    public static boolean allowed(String key) {
        return get(launcher, key);
    }

    private static boolean userOn(String key) {
        return user.has(key) ? user.get(key).getAsBoolean() : !OFF_BY_DEFAULT.contains(key);
    }

    public static boolean enabled(String key) {
        return allowed(key) && userOn(key);
    }

    public static void toggle(String key) {
        user.addProperty(key, !userOn(key));
        persist();
    }

    public static int getInt(String key, int def) {
        return user.has(key) ? user.get(key).getAsInt() : def;
    }

    public static void setInt(String key, int value) {
        user.addProperty(key, value);
        persist();
    }

    public static int key(String name, int def) {
        String k = "key." + name;
        return user.has(k) ? user.get(k).getAsInt() : def;
    }

    public static void setKey(String name, int code) {
        user.addProperty("key." + name, code);
        persist();
    }

    public static double[] pos(String id, double defX, double defY) {
        double x = user.has("pos." + id + ".x") ? user.get("pos." + id + ".x").getAsDouble() : defX;
        double y = user.has("pos." + id + ".y") ? user.get("pos." + id + ".y").getAsDouble() : defY;
        return new double[] { x, y };
    }

    public static void setPos(String id, double x, double y, boolean save) {
        user.addProperty("pos." + id + ".x", x);
        user.addProperty("pos." + id + ".y", y);
        if (save) persist();
    }

    public static double scale(String id) {
        return user.has("pos." + id + ".s") ? user.get("pos." + id + ".s").getAsDouble() : 1.0;
    }

    public static void setScale(String id, double s) {
        user.addProperty("pos." + id + ".s", Math.max(0.5, Math.min(3.0, Math.round(s * 10) / 10.0)));
        persist();
    }

    public static void setScaleLive(String id, double s, boolean save) {
        user.addProperty("pos." + id + ".s", Math.max(0.5, Math.min(3.0, Math.round(s * 20) / 20.0)));
        if (save) persist();
    }
    public static void resetPositions() {
        for (String k : new java.util.ArrayList<>(user.keySet())) if (k.startsWith("pos.")) user.remove(k);
        persist();
    }

    private static void persist() {
        try {
            Files.writeString(dir().resolve("prismfeatures-user.json"), new Gson().toJson(user));
        } catch (Exception e) {
            PrismFeatures.LOGGER.warn("Impossible d'enregistrer les choix du menu", e);
        }
    }
}
