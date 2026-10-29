package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fonctionnalités intégrées. Aucune dépendance à Fabric API : tout passe par des mixins.
 * Chaque fonctionnalité est isolée : si l'une échoue, elle est coupée (et loguée) sans toucher aux autres.
 */
public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    public static final int DEFAULT_KEY_NIGHT = Compat.KEY_N;
    public static final int DEFAULT_KEY_ZOOM = Compat.KEY_C;
    public static final int DEFAULT_KEY_MENU = Compat.KEY_RSHIFT;

    private static final Set<String> broken = new HashSet<>();
    private static final Set<String> okLogged = new HashSet<>();
    private static final Map<String, Boolean> wasDown = new HashMap<>();

    // --- capture d'une touche depuis le menu ---
    public static String listening = null;
    public static Runnable onBindingChanged = null;

    // --- états ---
    private static boolean nightVisionOn = false;
    private static boolean zoomActive = false;
    private static int savedFov = 70;
    private static boolean wasDead = false;
    public static BlockPos lastDeath = null;

    @Override
    public void onInitializeClient() {
        Config.load();
        LOGGER.info("[PrismFeatures] chargé");
    }

    // ------------------------------------------------------------------ utilitaires
    public static void safe(String name, Runnable r) {
        if (broken.contains(name)) return;
        try {
            r.run();
            if (okLogged.add(name)) LOGGER.info("[PrismFeatures] {} : OK", name);
        } catch (Throwable t) {
            broken.add(name);
            LOGGER.error("[PrismFeatures] {} : erreur, fonctionnalité coupée pour cette session", name, t);
        }
    }

    private static boolean keyDown(Minecraft client, int code) {
        return Compat.keyPressed(client, code);
    }

    /** Vrai une seule fois à l'appui de la touche. */
    private static boolean pressed(Minecraft client, String id, int code) {
        boolean down = keyDown(client, code);
        boolean was = wasDown.getOrDefault(id, false);
        wasDown.put(id, down);
        return down && !was;
    }

    public static String keyName(int code) {
        String n = Compat.keyDisplay(code);
        return n != null && !n.isEmpty() ? n.toUpperCase() : "Touche " + code;
    }

    // ------------------------------------------------------------------ tick
    public static void onTick(Minecraft client) {
        safe("capture-touche", () -> captureKey(client));
        if (client.player == null || client.level == null) {
            nightVisionOn = false;
            zoomActive = false;
            Extras.reset();
            Status.clear();
            return;
        }
        safe("night-vision", () -> tickNightVision(client));
        safe("zoom", () -> tickZoom(client));
        safe("point-de-mort", () -> tickDeath(client));
        safe("extras", Extras::tick);
        safe("statut", Status::tick);
        safe("menu", () -> {
            if (Compat.screen(client) == null && pressed(client, "menu", Config.key("menu", DEFAULT_KEY_MENU)))
                Compat.setScreen(client, new FeaturesScreen());
        });
    }

    /** Menu : « appuyez sur une touche ». Échap annule. */
    private static void captureKey(Minecraft client) {
        if (listening == null) return;
        for (int code : Compat.candidates()) {
            if (!keyDown(client, code)) continue;
            if (code != Compat.KEY_ESCAPE) Config.setKey(listening, code);
            listening = null;
            if (onBindingChanged != null) onBindingChanged.run();
            return;
        }
    }

    private static void tickNightVision(Minecraft client) {
        Player p = client.player;
        boolean allowed = Config.enabled(Config.NIGHT_VISION);
        if (Compat.screen(client) == null && listening == null && pressed(client, "nv", Config.key("nightVision", DEFAULT_KEY_NIGHT)) && allowed) {
            nightVisionOn = !nightVisionOn;
            Compat.overlayMessage(client, Component.literal("Night vision : " + (nightVisionOn ? "activée" : "désactivée")));
            if (!nightVisionOn) p.removeEffect(MobEffects.NIGHT_VISION);
        }
        if (!allowed && nightVisionOn) {
            nightVisionOn = false;
            p.removeEffect(MobEffects.NIGHT_VISION);
        }
        if (nightVisionOn && !p.hasEffect(MobEffects.NIGHT_VISION)) {
            // effet purement local, infini, sans particules ni icône
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
        }
    }

    private static void tickZoom(Minecraft client) {
        boolean want = Config.enabled(Config.ZOOM) && Compat.screen(client) == null && listening == null
                && keyDown(client, Config.key("zoom", DEFAULT_KEY_ZOOM));
        if (want && !zoomActive) {
            savedFov = client.options.fov().get();
            client.options.fov().set(30);
            zoomActive = true;
        } else if (!want && zoomActive) {
            client.options.fov().set(savedFov);
            zoomActive = false;
        }
    }

    /** Mémorise le lieu de la dernière mort (affiché à l'écran, jamais dans le chat). */
    private static void tickDeath(Minecraft client) {
        Player p = client.player;
        boolean dead = p.getHealth() <= 0.0f;
        if (dead && !wasDead) lastDeath = p.blockPosition();
        wasDead = dead;
    }

    // ------------------------------------------------------------------ shulker
    private static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    /** Nombre total d'un objet dans l'inventaire, y compris le contenu des shulkers qui s'y trouvent. */
    public static int countInInventory(Player p, ItemStack ref) {
        if (p == null || ref.isEmpty()) return ref.getCount();
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ref.getItem())) total += s.getCount();
            if (isShulker(s)) {
                ItemContainerContents c = s.get(DataComponents.CONTAINER);
                if (c != null) {
                    for (var tpl : c.nonEmptyItems()) { ItemStack inner = tpl.create(); if (inner.is(ref.getItem())) total += inner.getCount(); }
                }
            }
        }
        return total;
    }

    /** Survol d'un shulker dans un inventaire : son contenu s'affiche en grille. */
    public static void onSlotHover(GuiGraphicsExtractor ctx, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        if (!isShulker(stack)) return;
        onShulkerHover(ctx, stack, mouseX, mouseY);
    }

    /** Aperçu du contenu en grille à côté du curseur. */
    public static void onShulkerHover(GuiGraphicsExtractor ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!Config.enabled(Config.SHULKER)) return;
        safe("shulker", () -> {
            if (!isShulker(stack)) return;
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            for (var tpl : container.nonEmptyItems()) items.add(tpl.create());
            Minecraft client = Minecraft.getInstance();
            int rows = Math.max(1, (items.size() + 8) / 9);
            int gridH = rows * 18 + 8 + 14;
            int gridW = 9 * 18 + 8;
            int x = Math.max(4, Math.min(mouseX + 12, ctx.guiWidth() - gridW - 4));
            // au-dessus de l'infobulle de base (qui commence ~12 px au-dessus du curseur) ; en dessous s'il n'y a pas la place
            int y = mouseY - 12 - 6 - gridH;
            if (y < 4) y = Math.min(mouseY + 40, ctx.guiHeight() - gridH - 4);
            Compat.raise(ctx);
            drawGrid(ctx, client, items, x, y, items.isEmpty() ? "Shulker vide" : "Contenu du shulker");
        });
    }

    /** Petit panneau avec les objets en grille de 9 colonnes (icônes + quantités). */
    private static void drawGrid(GuiGraphicsExtractor ctx, Minecraft client, List<ItemStack> items, int x, int y, String title) {
        int cols = 9;
        int rows = Math.max(1, (items.size() + cols - 1) / cols);
        int top = 14;
        int w = cols * 18 + 8;
        int h = rows * 18 + 8 + top;
        ctx.fill(x, y, x + w, y + h, 0xE0101018);
        ctx.fill(x, y, x + w, y + 1, 0xFF7C5CFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFF7C5CFF);
        ctx.fill(x, y, x + 1, y + h, 0xFF7C5CFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFF7C5CFF);
        ctx.text(client.font, title, x + 5, y + 4, 0xFFFFFFFF, true);
        for (int i = 0; i < items.size(); i++) {
            int ix = x + 4 + (i % cols) * 18;
            int iy = y + top + 4 + (i / cols) * 18;
            ctx.item(items.get(i), ix, iy);
            Compat.overlay(ctx, client, items.get(i), ix, iy);
        }
    }

    // ------------------------------------------------------------------ HUD
    public static void onHud(GuiGraphicsExtractor ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || Compat.hudHidden(client)) return;
        Hud.render(ctx, client);
    }
}
