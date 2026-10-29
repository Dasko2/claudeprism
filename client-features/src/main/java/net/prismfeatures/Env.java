package net.prismfeatures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Accès au jeu pour {@link Extras} (noms Yarn, versions 1.19 à 1.21.x). */
public final class Env {
    private Env() {}

    public static final class Effect {
        public final String name;
        public final int amplifier;
        public final int ticks;
        Effect(String name, int amplifier, int ticks) { this.name = name; this.amplifier = amplifier; this.ticks = ticks; }
    }

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static boolean mouseDown(int button) {
        return GLFW.glfwGetMouseButton(Compat.handle(mc()), button) == GLFW.GLFW_PRESS;
    }

    public static boolean keyDown(String name) {
        int code = switch (name) {
            case "W" -> GLFW.GLFW_KEY_W;
            case "A" -> GLFW.GLFW_KEY_A;
            case "S" -> GLFW.GLFW_KEY_S;
            case "D" -> GLFW.GLFW_KEY_D;
            default -> GLFW.GLFW_KEY_SPACE;
        };
        return Compat.keyPressed(mc(), code);
    }

    public static boolean aimingAtEntity() {
        MinecraftClient c = mc();
        return c.currentScreen == null && c.crosshairTarget != null && c.crosshairTarget.getType() == HitResult.Type.ENTITY;
    }

    public static boolean justHurt() {
        PlayerEntity p = mc().player;
        return p != null && p.hurtTime > 0;
    }

    public static float health() {
        PlayerEntity p = mc().player;
        return p == null ? 0f : p.getHealth() + p.getAbsorptionAmount();
    }

    /** "" hors du jeu, "Solo" en solo, sinon l'adresse du serveur. */
    public static String server() {
        var c = mc();
        if (c.world == null) return "";
        var e = c.getCurrentServerEntry();
        if (e != null) return e.address;
        return c.getServer() != null ? "Solo" : "";
    }

    public static long day() {
        var w = mc().world;
        return w == null ? 1 : w.getTimeOfDay() / 24000L + 1;
    }

    public static float yaw() {
        PlayerEntity p = mc().player;
        return p == null ? 0f : p.getYaw();
    }

    public static int textWidth(String s) { return mc().textRenderer.getWidth(s); }

    public static List<Effect> effects() {
        List<Effect> out = new ArrayList<>();
        PlayerEntity p = mc().player;
        if (p == null) return out;
        for (StatusEffectInstance i : p.getStatusEffects()) out.add(new Effect(Compat.effectName(i), i.getAmplifier(), i.getDuration()));
        return out;
    }
}
