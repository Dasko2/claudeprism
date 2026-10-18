package net.prismfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deux sources :
 *  - config/prismfeatures.json        : autorisations écrites par le launcher (réglages globaux) ;
 *  - config/prismfeatures-user.json   : choix faits dans le menu en jeu (Maj droite).
 * Une fonctionnalité est active seulement si le launcher l'autorise ET si le joueur ne l'a pas coupée.
 */
public final class Config {
    public static final String NIGHT_VISION = "nightVision";
    public static final String MINIMAP = "minimap";
    public static final String HUD = "hud";
    public static final String CHEST = "chestPreview";
    public static final String SHULKER = "shulkerPreview";

    /** clé -> libellé affiché dans le menu */
    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put(NIGHT_VISION, "Night vision (touche N)");
        LABELS.put(MINIMAP, "Minimap");
        LABELS.put(HUD, "FPS et coordonnées");
        LABELS.put(CHEST, "Contenu des coffres");
        LABELS.put(SHULKER, "Contenu des shulkers");
    }

    private static JsonObject launcher = new JsonObject();
    private static JsonObject user = new JsonObject();

    public static boolean nightVision = true;
    public static boolean shulkerPreview = true;
    public static boolean chestPreview = true;
    public static boolean hud = true;
    public static boolean minimap = true;

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static void load() {
        launcher = read(dir().resolve("prismfeatures.json"));
        user = read(dir().resolve("prismfeatures-user.json"));
        recompute();
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

    /** Autorisé par le launcher ? (sinon le bouton du menu est grisé) */
    public static boolean allowed(String key) {
        return get(launcher, key);
    }

    public static boolean enabled(String key) {
        return allowed(key) && get(user, key);
    }

    public static void toggle(String key) {
        user.addProperty(key, !get(user, key));
        recompute();
        try {
            Files.writeString(dir().resolve("prismfeatures-user.json"), new Gson().toJson(user));
        } catch (Exception e) {
            PrismFeatures.LOGGER.warn("Impossible d'enregistrer les choix du menu", e);
        }
    }

    private static void recompute() {
        nightVision = enabled(NIGHT_VISION);
        shulkerPreview = enabled(SHULKER);
        chestPreview = enabled(CHEST);
        hud = enabled(HUD);
        minimap = enabled(MINIMAP);
    }
}
