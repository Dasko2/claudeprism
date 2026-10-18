package net.prismfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Lit config/prismfeatures.json (écrit par le launcher avant chaque lancement). */
public final class Config {
    public static boolean nightVision = true;
    public static boolean shulkerPreview = true;
    public static boolean chestPreview = true;
    public static boolean hud = true;
    public static boolean minimap = true;

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("prismfeatures.json");
        if (!Files.exists(file)) return;
        try {
            JsonObject json = new Gson().fromJson(Files.readString(file), JsonObject.class);
            nightVision = bool(json, "nightVision", nightVision);
            shulkerPreview = bool(json, "shulkerPreview", shulkerPreview);
            chestPreview = bool(json, "chestPreview", chestPreview);
            hud = bool(json, "hud", hud);
            minimap = bool(json, "minimap", minimap);
        } catch (Exception e) {
            PrismFeatures.LOGGER.warn("Config illisible, valeurs par défaut utilisées", e);
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        return o.has(key) ? o.get(key).getAsBoolean() : def;
    }
}
