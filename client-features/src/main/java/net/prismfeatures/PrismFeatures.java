package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
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
import java.util.UUID;

/**
 * Fonctionnalités intégrées. Aucune dépendance à Fabric API : tout passe par des mixins.
 * Chaque fonctionnalité est isolée : si l'une échoue, elle est coupée (et loguée) sans toucher aux autres.
 */
public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    public static final int DEFAULT_KEY_NIGHT = GLFW.GLFW_KEY_N;
    public static final int DEFAULT_KEY_ZOOM = GLFW.GLFW_KEY_C;
    public static final int DEFAULT_KEY_MENU = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public static final int DEFAULT_KEY_SHULKER = GLFW.GLFW_KEY_V;

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
    private static boolean shulkerLatch = false;

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

    private static boolean keyDown(MinecraftClient client, int code) {
        return Compat.keyPressed(client, code);
    }

    /** Vrai une seule fois à l'appui de la touche. */
    private static boolean pressed(MinecraftClient client, String id, int code) {
        boolean down = keyDown(client, code);
        boolean was = wasDown.getOrDefault(id, false);
        wasDown.put(id, down);
        return down && !was;
    }

    public static String keyName(int code) {
        switch (code) {
            case GLFW.GLFW_KEY_SPACE: return "Espace";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Maj gauche";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Maj droite";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Ctrl gauche";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Ctrl droite";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Alt gauche";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Alt droite";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Entrée";
            case GLFW.GLFW_KEY_UP: return "Flèche haut";
            case GLFW.GLFW_KEY_DOWN: return "Flèche bas";
            case GLFW.GLFW_KEY_LEFT: return "Flèche gauche";
            case GLFW.GLFW_KEY_RIGHT: return "Flèche droite";
            default:
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(code, 0);
        return n != null ? n.toUpperCase() : "Touche " + code;
    }

    // ------------------------------------------------------------------ tick
    public static void onTick(MinecraftClient client) {
        safe("capture-touche", () -> captureKey(client));
        if (client.player == null || client.world == null) {
            nightVisionOn = false;
            zoomActive = false;
            return;
        }
        safe("night-vision", () -> tickNightVision(client));
        safe("zoom", () -> tickZoom(client));
        safe("point-de-mort", () -> tickDeath(client));
        if (!keyDown(client, Config.key("shulkerOpen", DEFAULT_KEY_SHULKER))) shulkerLatch = false;
        safe("menu", () -> {
            if (client.currentScreen == null && pressed(client, "menu", Config.key("menu", DEFAULT_KEY_MENU)))
                client.setScreen(new FeaturesScreen());
        });
    }

    /** Menu : « appuyez sur une touche ». Échap annule. */
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
            p.sendMessage(Text.literal("Night vision : " + (nightVisionOn ? "activée" : "désactivée")), true);
            if (!nightVisionOn) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (!allowed && nightVisionOn) {
            nightVisionOn = false;
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (nightVisionOn && !p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            // effet purement local, infini, sans particules ni icône
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
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

    /** Mémorise le lieu de la dernière mort (affiché à l'écran, jamais dans le chat). */
    private static void tickDeath(MinecraftClient client) {
        PlayerEntity p = client.player;
        boolean dead = p.getHealth() <= 0.0f;
        if (dead && !wasDead) lastDeath = p.getBlockPos();
        wasDead = dead;
    }

    // ------------------------------------------------------------------ shulker
    private static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    /** Nombre total d'un objet dans l'inventaire, y compris le contenu des shulkers qui s'y trouvent. */
    public static int countInInventory(PlayerEntity p, ItemStack ref) {
        if (p == null || ref.isEmpty()) return ref.getCount();
        int total = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (s.isEmpty()) continue;
            if (s.isOf(ref.getItem())) total += s.getCount();
            if (isShulker(s)) {
                ContainerComponent c = s.get(DataComponentTypes.CONTAINER);
                if (c != null) {
                    total += c.streamNonEmpty().filter(inner -> inner.isOf(ref.getItem())).mapToInt(ItemStack::getCount).sum();
                }
            }
        }
        return total;
    }

    /** Solo uniquement : ouvre le contenu d'un shulker depuis l'inventaire, sans le poser. */
    private static void openShulker(MinecraftClient client, int slotId) {
        MinecraftServer server = client.getServer();
        if (server == null) {
            client.player.sendMessage(Text.literal("Ouvrir un shulker depuis l'inventaire : solo uniquement"), true);
            return;
        }
        final UUID id = client.player.getUuid();
        server.execute(() -> {
            ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
            if (sp == null) return;
            final ItemStack target = sp.currentScreenHandler.getSlot(slotId).getStack();
            if (!isShulker(target)) return;
            final SimpleInventory inv = new SimpleInventory(27);
            ContainerComponent comp = target.get(DataComponentTypes.CONTAINER);
            if (comp != null) {
                DefaultedList<ItemStack> tmp = DefaultedList.ofSize(27, ItemStack.EMPTY);
                comp.copyTo(tmp);
                for (int i = 0; i < 27; i++) inv.setStack(i, tmp.get(i));
            }
            sp.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInv, player) ->
                    new ShulkerBoxScreenHandler(syncId, playerInv, inv) {
                        @Override
                        public void onClosed(PlayerEntity pl) {
                            super.onClosed(pl);
                            List<ItemStack> out = new ArrayList<>();
                            for (int i = 0; i < 27; i++) out.add(inv.getStack(i).copy());
                            target.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(out));
                        }
                    }, target.getName()));
        });
    }

    /** Survol d'un shulker dans un inventaire : son contenu s'affiche en grille ; la touche (V) l'ouvre en solo. */
    public static void onSlotHover(DrawContext ctx, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getStack();
        if (!isShulker(stack)) return;
        safe("shulker-ouvrir", () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (keyDown(client, Config.key("shulkerOpen", DEFAULT_KEY_SHULKER)) && !shulkerLatch) {
                shulkerLatch = true;
                openShulker(client, slot.id);
            }
        });
        onShulkerHover(ctx, stack, mouseX, mouseY);
    }

    /** Aperçu du contenu en grille à côté du curseur. */
    public static void onShulkerHover(DrawContext ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!Config.enabled(Config.SHULKER)) return;
        safe("shulker", () -> {
            if (!isShulker(stack)) return;
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            container.streamNonEmpty().forEach(items::add);
            MinecraftClient client = MinecraftClient.getInstance();
            int x = Math.min(mouseX + 12, ctx.getScaledWindowWidth() - 9 * 18 - 10);
            int y = Math.min(mouseY + 44, ctx.getScaledWindowHeight() - 4 * 18 - 30);
            drawGrid(ctx, client, items, x, y, items.isEmpty() ? "Shulker vide" : "Contenu du shulker");
        });
    }

    /** Petit panneau avec les objets en grille de 9 colonnes (icônes + quantités). */
    private static void drawGrid(DrawContext ctx, MinecraftClient client, List<ItemStack> items, int x, int y, String title) {
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
        ctx.drawTextWithShadow(client.textRenderer, title, x + 5, y + 4, 0xFFFFFFFF);
        for (int i = 0; i < items.size(); i++) {
            int ix = x + 4 + (i % cols) * 18;
            int iy = y + top + 4 + (i / cols) * 18;
            ctx.drawItem(items.get(i), ix, iy);
            ctx.drawStackOverlay(client.textRenderer, items.get(i), ix, iy);
        }
    }

    // ------------------------------------------------------------------ HUD
    public static void onHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        Hud.render(ctx, client);
    }
}
