package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    public static void onTick(Minecraft client) {
        safe("capture-touche", () -> captureKey(client));
        if (client.player == null || client.level == null) {
            nightVisionOn = false;
            zoomActive = false;
            return;
        }
        safe("night-vision", () -> tickNightVision(client));
        safe("zoom", () -> tickZoom(client));
        safe("point-de-mort", () -> tickDeath(client));
        if (!keyDown(client, Config.key("shulkerOpen", DEFAULT_KEY_SHULKER))) shulkerLatch = false;
        safe("menu", () -> {
            if (client.screen == null && pressed(client, "menu", Config.key("menu", DEFAULT_KEY_MENU)))
                client.setScreen(new FeaturesScreen());
        });
    }

    /** Menu : « appuyez sur une touche ». Échap annule. */
    private static void captureKey(Minecraft client) {
        if (listening == null) return;
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            if (!keyDown(client, code)) continue;
            if (code != GLFW.GLFW_KEY_ESCAPE) Config.setKey(listening, code);
            listening = null;
            if (onBindingChanged != null) onBindingChanged.run();
            return;
        }
    }

    private static void tickNightVision(Minecraft client) {
        Player p = client.player;
        boolean allowed = Config.enabled(Config.NIGHT_VISION);
        if (client.screen == null && listening == null && pressed(client, "nv", Config.key("nightVision", DEFAULT_KEY_NIGHT)) && allowed) {
            nightVisionOn = !nightVisionOn;
            p.displayClientMessage(Component.literal("Night vision : " + (nightVisionOn ? "activée" : "désactivée")), true);
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
        boolean want = Config.enabled(Config.ZOOM) && client.screen == null && listening == null
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
                    total += c.nonEmptyStream().filter(inner -> inner.is(ref.getItem())).mapToInt(ItemStack::getCount).sum();
                }
            }
        }
        return total;
    }

    /** Solo uniquement : ouvre le contenu d'un shulker depuis l'inventaire, sans le poser. */
    private static void openShulker(Minecraft client, int slotId) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            client.player.displayClientMessage(Component.literal("Ouvrir un shulker depuis l'inventaire : solo uniquement"), true);
            return;
        }
        final UUID id = client.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) return;
            final ItemStack target = sp.containerMenu.getSlot(slotId).getItem();
            if (!isShulker(target)) return;
            final SimpleContainer inv = new SimpleContainer(27);
            ItemContainerContents comp = target.get(DataComponents.CONTAINER);
            if (comp != null) {
                NonNullList<ItemStack> tmp = NonNullList.withSize(27, ItemStack.EMPTY);
                comp.copyInto(tmp);
                for (int i = 0; i < 27; i++) inv.setItem(i, tmp.get(i));
            }
            sp.openMenu(new SimpleMenuProvider((syncId, playerInv, player) ->
                    new ShulkerBoxMenu(syncId, playerInv, inv) {
                        @Override
                        public void removed(Player pl) {
                            super.removed(pl);
                            List<ItemStack> out = new ArrayList<>();
                            for (int i = 0; i < 27; i++) out.add(inv.getItem(i).copy());
                            target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(out));
                        }
                    }, target.getHoverName()));
        });
    }

    /** Survol d'un shulker dans un inventaire : son contenu s'affiche en grille ; la touche (V) l'ouvre en solo. */
    public static void onSlotHover(GuiGraphics ctx, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        if (!isShulker(stack)) return;
        safe("shulker-ouvrir", () -> {
            Minecraft client = Minecraft.getInstance();
            if (keyDown(client, Config.key("shulkerOpen", DEFAULT_KEY_SHULKER)) && !shulkerLatch) {
                shulkerLatch = true;
                openShulker(client, slot.index);
            }
        });
        onShulkerHover(ctx, stack, mouseX, mouseY);
    }

    /** Aperçu du contenu en grille à côté du curseur. */
    public static void onShulkerHover(GuiGraphics ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!Config.enabled(Config.SHULKER)) return;
        safe("shulker", () -> {
            if (!isShulker(stack)) return;
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            container.nonEmptyStream().forEach(items::add);
            Minecraft client = Minecraft.getInstance();
            int x = Math.min(mouseX + 12, ctx.guiWidth() - 9 * 18 - 10);
            int y = Math.min(mouseY + 44, ctx.guiHeight() - 4 * 18 - 30);
            drawGrid(ctx, client, items, x, y, items.isEmpty() ? "Shulker vide" : "Contenu du shulker");
        });
    }

    /** Petit panneau avec les objets en grille de 9 colonnes (icônes + quantités). */
    private static void drawGrid(GuiGraphics ctx, Minecraft client, List<ItemStack> items, int x, int y, String title) {
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
        ctx.drawString(client.font, title, x + 5, y + 4, 0xFFFFFFFF);
        for (int i = 0; i < items.size(); i++) {
            int ix = x + 4 + (i % cols) * 18;
            int iy = y + top + 4 + (i / cols) * 18;
            ctx.renderItem(items.get(i), ix, iy);
            Compat.overlay(ctx, client, items.get(i), ix, iy);
        }
    }

    // ------------------------------------------------------------------ HUD
    public static void onHud(GuiGraphics ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui) return;
        Hud.render(ctx, client);
    }
}
