package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    public static final int DEFAULT_KEY_NIGHT = GLFW.GLFW_KEY_N;
    public static final int DEFAULT_KEY_ZOOM = GLFW.GLFW_KEY_C;
    public static final int DEFAULT_KEY_MENU = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private static final Set<String> broken = new HashSet<>();
    private static final Set<String> okLogged = new HashSet<>();
    private static final Map<String, Boolean> wasDown = new HashMap<>();

    public static String listening = null;
    public static Runnable onBindingChanged = null;

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

    private static boolean keyDown(MinecraftClient client, int code) {
        return Compat.keyPressed(client, code);
    }

    private static boolean pressed(MinecraftClient client, String id, int code) {
        boolean down = keyDown(client, code);
        boolean was = wasDown.getOrDefault(id, false);
        wasDown.put(id, down);
        return down && !was;
    }

    public static String keyName(int code) {
        switch (code) {
            case GLFW.GLFW_KEY_SPACE: return T.t("Espace");
            case GLFW.GLFW_KEY_LEFT_SHIFT: return T.t("Maj gauche");
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return T.t("Maj droite");
            case GLFW.GLFW_KEY_LEFT_CONTROL: return T.t("Ctrl gauche");
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return T.t("Ctrl droite");
            case GLFW.GLFW_KEY_LEFT_ALT: return T.t("Alt gauche");
            case GLFW.GLFW_KEY_RIGHT_ALT: return T.t("Alt droite");
            case GLFW.GLFW_KEY_TAB: return T.t("Tab");
            case GLFW.GLFW_KEY_ENTER: return T.t("Entrée");
            case GLFW.GLFW_KEY_UP: return T.t("Flèche haut");
            case GLFW.GLFW_KEY_DOWN: return T.t("Flèche bas");
            case GLFW.GLFW_KEY_LEFT: return T.t("Flèche gauche");
            case GLFW.GLFW_KEY_RIGHT: return T.t("Flèche droite");
            default:
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(code, 0);
        return n != null ? n.toUpperCase() : T.t("Touche ") + code;
    }

    public static void onTick(MinecraftClient client) {
        safe("capture-touche", () -> captureKey(client));
        if (client.player == null || client.world == null) {
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
            if (client.currentScreen == null && pressed(client, "menu", Config.key("menu", DEFAULT_KEY_MENU)))
                client.setScreen(new FeaturesScreen());
        });
    }

    private static void captureKey(MinecraftClient client) {
        if (listening == null) return;
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            if (!keyDown(client, code)) continue;
            if (code != GLFW.GLFW_KEY_ESCAPE) Config.setKey(listening, code);
            listening = null;
            if (onBindingChanged != null) onBindingChanged.run();
            return;
        }
    }

    private static void tickNightVision(MinecraftClient client) {
        PlayerEntity p = client.player;
        boolean allowed = Config.enabled(Config.NIGHT_VISION);
        if (client.currentScreen == null && listening == null && pressed(client, "nv", Config.key("nightVision", DEFAULT_KEY_NIGHT)) && allowed) {
            nightVisionOn = !nightVisionOn;
            p.sendMessage(Text.literal(T.t("Night vision : ") + (nightVisionOn ? T.t("activée") : T.t("désactivée"))), true);
            if (!nightVisionOn) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (!allowed && nightVisionOn) {
            nightVisionOn = false;
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (nightVisionOn && (!p.hasStatusEffect(StatusEffects.NIGHT_VISION) || p.getStatusEffect(StatusEffects.NIGHT_VISION).getDuration() < 2000)) {

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000000, 0, false, false, false));
        }
    }

    private static void tickZoom(MinecraftClient client) {
        boolean want = Config.enabled(Config.ZOOM) && client.currentScreen == null && listening == null
                && keyDown(client, Config.key("zoom", DEFAULT_KEY_ZOOM));
        if (want && !zoomActive) {
            savedFov = client.options.getFov().getValue();
            client.options.getFov().setValue(30);
            zoomActive = true;
        } else if (!want && zoomActive) {
            client.options.getFov().setValue(savedFov);
            zoomActive = false;
        }
    }

    private static void tickDeath(MinecraftClient client) {
        PlayerEntity p = client.player;
        boolean dead = p.getHealth() <= 0.0f;
        if (dead && !wasDead) lastDeath = p.getBlockPos();
        wasDead = dead;
    }

    private static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    public static int countInInventory(PlayerEntity p, ItemStack ref) {
        if (p == null || ref.isEmpty()) return ref.getCount();
        int total = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isEmpty()) continue;
            if (s.isOf(ref.getItem())) total += s.getCount();
            if (isShulker(s)) {
                for (ItemStack inner : Compat.shulkerItems(s)) if (inner.isOf(ref.getItem())) total += inner.getCount();
            }
        }
        return total;
    }

    private static long lastHoverDraw = 0L;

    public static void onSlotHover(Gfx ctx, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getStack();
        if (!isShulker(stack)) return;
        long now = System.nanoTime();
        if (now - lastHoverDraw < 3_000_000L) return;
        lastHoverDraw = now;
        onShulkerHover(ctx, stack, mouseX, mouseY);
    }

    public static void onShulkerHover(Gfx ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!Config.enabled(Config.SHULKER)) return;
        safe("shulker", () -> {
            if (!isShulker(stack)) return;
            List<ItemStack> items = Compat.shulkerItems(stack);
            MinecraftClient client = MinecraftClient.getInstance();
            int rows = Math.max(1, (items.size() + 8) / 9);
            int gridH = rows * 18 + 8 + 14;
            int gridW = 9 * 18 + 8;
            int x = Math.max(4, Math.min(mouseX + 12, ctx.width() - gridW - 4));

            int y = mouseY - 12 - 6 - gridH;
            if (y < 4) y = Math.min(mouseY + 40, ctx.height() - gridH - 4);
            ctx.raise();
            drawGrid(ctx, client, items, x, y, items.isEmpty() ? T.t("Shulker vide") : T.t("Contenu du shulker"));
        });
    }

    private static void drawGrid(Gfx ctx, MinecraftClient client, List<ItemStack> items, int x, int y, String title) {
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
        ctx.text(title, x + 5, y + 4, 0xFFFFFFFF);
        for (int i = 0; i < items.size(); i++) {
            int ix = x + 4 + (i % cols) * 18;
            int iy = y + top + 4 + (i / cols) * 18;
            ctx.item(items.get(i), ix, iy);
            ctx.overlay(items.get(i), ix, iy);
        }
    }

    public static void onHud(Gfx ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        Hud.render(ctx, client);
    }
}
