package net.prismfeatures.agent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class PrismAgent {
    private PrismAgent() {}

    public static void premain(String args, Instrumentation inst) {
        try {
            Map<String, String> opts = parse(args);
            String loader = opts.getOrDefault("loader", "");
            String mc = opts.getOrDefault("mc", "");
            String cache = opts.get("cache");
            log("loader=" + loader + " mc=" + mc);

            String resource = findPayload(loader, mc);
            if (resource == null) {
                log("aucune charge utile pour " + loader + " " + mc + " : fonctionnalités désactivées pour ce lancement");
                return;
            }
            File jar = extract(resource, cache == null ? new File(System.getProperty("java.io.tmpdir"), "prismfeatures") : new File(cache));
            attach(loader, jar);
        } catch (Throwable t) {

            log("erreur ignorée : " + t);
        }
    }

    private static void attach(String loader, File jar) {
        switch (loader) {
            case "fabric":
                addToProperty("fabric.addMods", jar);
                log("payload Fabric chargé : " + jar);
                break;
            case "quilt":
                addToProperty("loader.addMods", jar);
                addToProperty("fabric.addMods", jar);
                log("payload Quilt chargé : " + jar);
                break;
            default:

                log("loader « " + loader + " » : chargement caché pas encore disponible");
        }
    }

    private static void addToProperty(String key, File jar) {
        String current = System.getProperty(key);
        String path = jar.getAbsolutePath();
        System.setProperty(key, current == null || current.isEmpty() ? path : current + File.pathSeparator + path);
    }

    private static String findPayload(String loader, String mc) {
        if (loader.isEmpty()) return null;
        String[] candidates = { loader + "-" + mc, loader + "-" + majorMinor(mc), loader + "-any" };
        for (String name : candidates) {
            String res = "/payloads/" + name + ".jar";
            if (PrismAgent.class.getResource(res) != null) return res;
        }
        return null;
    }

    private static String majorMinor(String mc) {
        String[] parts = mc.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : mc;
    }

    private static File extract(String resource, File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("dossier de cache impossible à créer : " + dir);
        File out = new File(dir, resource.substring(resource.lastIndexOf('/') + 1));
        try (InputStream in = PrismAgent.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("ressource introuvable : " + resource);
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return out;
    }

    private static Map<String, String> parse(String args) {
        Map<String, String> map = new HashMap<String, String>();
        if (args == null) return map;
        for (String part : args.split("\\|")) {
            int eq = part.indexOf('=');
            if (eq > 0) map.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
        }
        return map;
    }

    private static void log(String msg) {
        System.out.println("[PrismFeatures] " + msg);
    }
}
