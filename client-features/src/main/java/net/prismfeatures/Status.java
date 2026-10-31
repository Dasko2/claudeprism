package net.prismfeatures;

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

    public static void clear() {
        if (last != null && !last.isEmpty()) {
            last = "";
            Config.writeStatus("");
        }
        counter = 0;
    }
}
