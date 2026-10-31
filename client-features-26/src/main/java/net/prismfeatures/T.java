package net.prismfeatures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class T {
    private T() {}

    private static final Map<String, String> base = load("fr_fr");
    private static final Map<String, String> lang = pick();

    private static Map<String, String> pick() {
        Locale l = Locale.getDefault();
        String full = (l.getLanguage() + "_" + l.getCountry()).toLowerCase(Locale.ROOT);
        Map<String, String> m = load(full);
        if (m.isEmpty()) m = load(l.getLanguage().toLowerCase(Locale.ROOT));
        return m;
    }

    private static Map<String, String> load(String code) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = T.class.getResourceAsStream("/prismlang/" + code + ".json")) {
            if (in == null) return out;
            JsonObject o = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            for (Map.Entry<String, com.google.gson.JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        } catch (Exception ignored) {
        }
        return out;
    }

    public static String t(String key) {
        String v = lang.get(key);
        if (v != null) return v;
        v = base.get(key);
        return v != null ? v : key;
    }
}
