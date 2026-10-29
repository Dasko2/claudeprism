package net.prismfeatures;

/** Publie le serveur actuel (toutes les 5 s, seulement si ça change). */
public final class Status {
    private Status() {}

    private static String last = null;
    private static int counter = 0;

    public static void tick() {
        if (counter++ % 100 != 0) return;
        String now = Env.server();
        if (!now.equals(last)) {
            last = now;
            Config.writeStatus(now);
        }
    }

    /** À la fermeture du monde. */
    public static void clear() {
        if (last != null && !last.isEmpty()) {
            last = "";
            Config.writeStatus("");
        }
        counter = 0;
    }
}
