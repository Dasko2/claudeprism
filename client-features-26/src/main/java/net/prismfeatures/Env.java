package net.prismfeatures;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** Accès au jeu pour Extras (noms Mojang). */
public final class Env {
    private Env() {}

    public static final class Effect {
        public final String name;
        public final int amplifier;
        public final int ticks;
        Effect(String name, int amplifier, int ticks) { this.name = name; this.amplifier = amplifier; this.ticks = ticks; }
    }

    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static boolean mouseDown(int button) {
        Minecraft c = mc();
        if (c.mouseHandler == null) return false;
        return button == 0 ? Compat.leftMouseDown(c) : c.mouseHandler.isRightPressed();
    }

    public static boolean keyDown(String k) {
        int code = switch (k) {
            case "W" -> InputConstants.KEY_W;
            case "A" -> InputConstants.KEY_A;
            case "S" -> InputConstants.KEY_S;
            case "D" -> InputConstants.KEY_D;
            default -> InputConstants.KEY_SPACE;
        };
        Minecraft c = mc();
        return c.screen == null && Compat.keyPressed(c, code);
    }

    public static boolean aimingAtEntity() {
        Minecraft c = mc();
        return c.screen == null && c.hitResult != null && c.hitResult.getType() == HitResult.Type.ENTITY;
    }

    public static boolean justHurt() { return mc().player != null && mc().player.hurtTime > 0; }

    public static float health() {
        var p = mc().player;
        return p == null ? 0f : p.getHealth() + p.getAbsorptionAmount();
    }

    public static float yaw() { return mc().player == null ? 0f : mc().player.getYRot(); }

    public static int textWidth(String s) { return mc().font.width(s); }

    public static List<Effect> effects() {
        List<Effect> out = new ArrayList<>();
        var p = mc().player;
        if (p == null) return out;
        for (MobEffectInstance i : p.getActiveEffects())
            out.add(new Effect(i.getEffect().value().getDisplayName().getString(), i.getAmplifier(), i.isInfinite() ? -1 : i.getDuration()));
        return out;
    }
}
