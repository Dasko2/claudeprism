package net.prismfeatures;

/** Regarder autour sans tourner le joueur : décalages de caméra tant que la touche est maintenue. */
public final class Freelook {
    private Freelook() {}

    public static volatile boolean active = false;
    public static float dyaw = 0f;
    public static float dpitch = 0f;

    public static void set(boolean on) {
        if (!on) {
            dyaw = 0f;
            dpitch = 0f;
        }
        active = on;
    }

    /** Appelé avec les mêmes deltas que la souris (unités du jeu). */
    public static void look(double yawDelta, double pitchDelta) {
        dyaw += (float) (yawDelta * 0.15);
        dpitch += (float) (pitchDelta * 0.15);
        if (dpitch > 90f) dpitch = 90f;
        if (dpitch < -90f) dpitch = -90f;
    }
}
