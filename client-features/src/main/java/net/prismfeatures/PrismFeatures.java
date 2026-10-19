package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fonctionnalités intégrées. Aucune dépendance à Fabric API : tout passe par des mixins.
 * Chaque fonctionnalité est isolée : si l'une échoue, elle est coupée (et loguée) sans toucher aux autres.
 */
public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    public static final int DEFAULT_KEY_NIGHT = GLFW.GLFW_KEY_N;
    public static final int DEFAULT_KEY_ZOOM = GLFW.GLFW_KEY_C;
    public static final int DEFAULT_KEY_MENU = GLFW.GLFW_KEY_RIGHT_SHIFT;

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
    private static BlockPos lastDeath = null;
    private static String lastDeathDim = "";

    private static BlockPos lastClicked = null;
    private static final Map<BlockPos, List<ItemStack>> chestMemory = new HashMap<>();

    // minimap
    private static final int MAP_CELLS = 48;
    private static final int MAP_CELL_PX = 2;
    private static final int[] mapColors = new int[MAP_CELLS * MAP_CELLS];
    private static boolean mapReady = false;
    private static int mapTick = 0;

    @Override
    public void onInitializeClient() {
        Config.load();
        LOGGER.info("[PrismFeatures] chargé : nightVision={} minimap={} coffres={} shulkers={} zoom={} sprint={} armure={}",
                Config.nightVision, Config.minimap, Config.chestPreview, Config.shulkerPreview, Config.zoom, Config.autoSprint, Config.armor);
    }

    // ------------------------------------------------------------------ utilitaires
    private static void safe(String name, Runnable r) {
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
        return InputUtil.isKeyPressed(client.getWindow(), code);
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
        safe("sprint-auto", () -> tickSprint(client));
        safe("point-de-mort", () -> tickDeath(client));
        safe("menu", () -> {
            if (client.currentScreen == null && pressed(client, "menu", Config.key("menu", DEFAULT_KEY_MENU)))
                client.setScreen(new FeaturesScreen());
        });
        safe("coffres-memoire", () -> tickChestMemory(client));
        safe("minimap-calcul", () -> {
            if (Config.minimap && (mapTick++ % 10 == 0)) rebuildMap(client);
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
        if (client.currentScreen == null && listening == null && pressed(client, "nv", Config.key("nightVision", DEFAULT_KEY_NIGHT)) && Config.nightVision) {
            nightVisionOn = !nightVisionOn;
            p.sendMessage(Text.literal("Night vision : " + (nightVisionOn ? "activée" : "désactivée")), true);
            if (!nightVisionOn) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (!Config.nightVision && nightVisionOn) {
            nightVisionOn = false;
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (nightVisionOn && !p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            // effet purement local, infini, sans particules ni icône
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
        }
    }

    private static void tickZoom(MinecraftClient client) {
        boolean want = Config.zoom && client.currentScreen == null && listening == null && keyDown(client, Config.key("zoom", DEFAULT_KEY_ZOOM));
        if (want && !zoomActive) {
            savedFov = client.options.getFov().getValue();
            client.options.getFov().setValue(30);
            zoomActive = true;
        } else if (!want && zoomActive) {
            client.options.getFov().setValue(savedFov);
            zoomActive = false;
        }
    }

    private static void tickSprint(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (!Config.autoSprint || client.currentScreen != null) return;
        if (client.options.forwardKey.isPressed() && !p.isSneaking() && !p.isUsingItem() && !p.horizontalCollision
                && p.getHungerManager().getFoodLevel() > 6) {
            p.setSprinting(true);
        }
    }

    private static void tickDeath(MinecraftClient client) {
        PlayerEntity p = client.player;
        boolean dead = p.getHealth() <= 0.0f;
        if (dead && !wasDead && Config.deathPoint) {
            lastDeath = p.getBlockPos();
            lastDeathDim = client.world.getRegistryKey().getValue().getPath();
            p.sendMessage(Text.literal("Vous êtes mort en " + lastDeath.getX() + " " + lastDeath.getY() + " " + lastDeath.getZ()
                    + " (" + lastDeathDim + ")"), false);
        }
        wasDead = dead;
    }

    // ------------------------------------------------------------------ coffres
    public static void onBlockUsed(BlockPos pos) {
        try {
            lastClicked = pos.toImmutable();
        } catch (Throwable ignored) {
        }
    }

    private static void tickChestMemory(MinecraftClient client) {
        if (!Config.chestPreview || lastClicked == null || !(client.currentScreen instanceof HandledScreen<?> screen)) return;
        ScreenHandler handler = screen.getScreenHandler();
        int size = -1;
        if (handler instanceof GenericContainerScreenHandler g) size = g.getRows() * 9;
        else if (handler instanceof ShulkerBoxScreenHandler) size = 27;
        else if (handler instanceof Generic3x3ContainerScreenHandler) size = 9;
        else if (handler instanceof HopperScreenHandler) size = 5;
        if (size <= 0) return;
        List<ItemStack> copy = new ArrayList<>();
        for (int i = 0; i < size && i < handler.slots.size(); i++) {
            ItemStack s = handler.slots.get(i).getStack();
            if (!s.isEmpty()) copy.add(s.copy());
        }
        chestMemory.put(lastClicked, copy);
        // coffre double : l'autre moitié partage le même contenu
        BlockPos other = otherHalf(client.world, lastClicked);
        if (other != null) chestMemory.put(other, copy);
    }

    private static BlockPos otherHalf(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            Direction d = ChestBlock.getFacing(state);
            return pos.offset(d);
        }
        return null;
    }

    /** Solo : contenu réel lu directement dans le monde du serveur interne, sans ouvrir. Sinon : mémoire des conteneurs déjà ouverts. */
    private static List<ItemStack> contentsAt(MinecraftClient client, BlockPos pos) {
        MinecraftServer server = client.getServer();
        if (server != null) {
            try {
                ServerWorld sw = server.getWorld(client.world.getRegistryKey());
                if (sw != null) {
                    BlockState state = sw.getBlockState(pos);
                    Inventory inv = null;
                    if (state.getBlock() instanceof ChestBlock cb) inv = ChestBlock.getInventory(cb, state, sw, pos, true);
                    else {
                        BlockEntity be = sw.getBlockEntity(pos);
                        if (be instanceof Inventory i) inv = i;
                    }
                    if (inv != null) {
                        List<ItemStack> out = new ArrayList<>();
                        for (int i = 0; i < inv.size(); i++) {
                            ItemStack s = inv.getStack(i);
                            if (!s.isEmpty()) out.add(s.copy());
                        }
                        return out;
                    }
                }
            } catch (Throwable ignored) {
                // lecture concurrente : on retombe sur la mémoire
            }
        }
        return chestMemory.get(pos);
    }

    // ------------------------------------------------------------------ shulker
    private static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    public static void onShulkerHover(DrawContext ctx, ItemStack stack, int mouseX, int mouseY) {
        if (!Config.shulkerPreview) return;
        safe("shulker", () -> {
            if (!isShulker(stack)) return;
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            container.streamNonEmpty().forEach(items::add);
            MinecraftClient client = MinecraftClient.getInstance();
            int x = Math.min(mouseX + 12, ctx.getScaledWindowWidth() - 9 * 18 - 10);
            int y = Math.min(mouseY + 44, ctx.getScaledWindowHeight() - 4 * 18 - 30);
            drawGrid(ctx, client, items, x, y, "Contenu du shulker");
        });
    }

    // ------------------------------------------------------------------ HUD
    public static void onHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) return;
        safe("hud-infos", () -> {
            if (Config.hud) drawInfos(ctx, client);
        });
        safe("hud-armure", () -> {
            if (Config.armor) drawArmor(ctx, client);
        });
        safe("minimap", () -> {
            if (Config.minimap && mapReady) drawMap(ctx, client);
        });
        safe("coffres-vue", () -> {
            if (Config.chestPreview) drawChestPreview(ctx, client);
        });
    }

    private static void drawInfos(DrawContext ctx, MinecraftClient client) {
        PlayerEntity p = client.player;
        BlockPos pos = p.getBlockPos();
        String biome = client.world.getBiome(pos).getKey().map(k -> k.getValue().getPath().replace('_', ' ')).orElse("?");
        String dir = switch (p.getHorizontalFacing()) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            default -> "O";
        };
        List<String> lines = new ArrayList<>();
        lines.add(client.getCurrentFps() + " FPS");
        lines.add("XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  " + dir);
        lines.add("Biome : " + biome);
        if (client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (entry != null && client.getServer() == null) lines.add("Ping : " + entry.getLatency() + " ms");
        }
        lines.add("Heure : " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (Config.deathPoint && lastDeath != null)
            lines.add("Mort : " + lastDeath.getX() + " " + lastDeath.getY() + " " + lastDeath.getZ());
        int y = 4;
        for (String l : lines) {
            ctx.drawTextWithShadow(client.textRenderer, l, 4, y, 0xFFFFFFFF);
            y += 11;
        }
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient client) {
        PlayerEntity p = client.player;
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND };
        int x = 4;
        int y = ctx.getScaledWindowHeight() / 2 - 45;
        for (EquipmentSlot slot : slots) {
            ItemStack s = p.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            ctx.drawItem(s, x, y);
            ctx.drawStackOverlay(client.textRenderer, s, x, y);
            if (s.isDamageable()) {
                int left = s.getMaxDamage() - s.getDamage();
                ctx.drawTextWithShadow(client.textRenderer, left + "/" + s.getMaxDamage(), x + 20, y + 4, 0xFFFFFFFF);
            }
            y += 20;
        }
    }

    private static void drawChestPreview(DrawContext ctx, MinecraftClient client) {
        if (client.currentScreen != null || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        List<ItemStack> items = contentsAt(client, hit.getBlockPos());
        if (items == null) return;
        drawGrid(ctx, client, items, ctx.getScaledWindowWidth() / 2 + 14, ctx.getScaledWindowHeight() / 2 + 14, items.isEmpty() ? "Vide" : "Contenu");
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

    // ------------------------------------------------------------------ minimap
    private static void rebuildMap(MinecraftClient client) {
        World world = client.world;
        BlockPos center = client.player.getBlockPos();
        boolean ceiling = world.getDimension().hasCeiling();
        int half = MAP_CELLS / 2;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int cz = 0; cz < MAP_CELLS; cz++) {
            for (int cx = 0; cx < MAP_CELLS; cx++) {
                int wx = center.getX() - half + cx;
                int wz = center.getZ() - half + cz;
                int color = 0xFF101018; // zone non chargée
                if (world.isChunkLoaded(wx >> 4, wz >> 4)) {
                    int y;
                    if (ceiling) {
                        // Nether : pas de surface, on cherche le sol sous le joueur
                        y = center.getY() + 1;
                        while (y > world.getBottomY() && world.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
                    } else {
                        y = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                    }
                    pos.set(wx, y, wz);
                    int rgb = world.getBlockState(pos).getMapColor(world, pos).color;
                    int yn = ceiling ? y : world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz - 1) - 1;
                    float shade = y > yn ? 1.15f : (y < yn ? 0.8f : 1.0f);
                    int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade));
                    int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade));
                    int b = Math.min(255, (int) ((rgb & 0xFF) * shade));
                    color = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                mapColors[cz * MAP_CELLS + cx] = color;
            }
        }
        mapReady = true;
    }

    private static void drawMap(DrawContext ctx, MinecraftClient client) {
        int size = MAP_CELLS * MAP_CELL_PX;
        int x0 = ctx.getScaledWindowWidth() - size - 8;
        int y0 = 8;
        ctx.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xCC000000);
        for (int cz = 0; cz < MAP_CELLS; cz++) {
            for (int cx = 0; cx < MAP_CELLS; cx++) {
                int px = x0 + cx * MAP_CELL_PX;
                int py = y0 + cz * MAP_CELL_PX;
                ctx.fill(px, py, px + MAP_CELL_PX, py + MAP_CELL_PX, mapColors[cz * MAP_CELLS + cx]);
            }
        }
        int c = size / 2;
        ctx.fill(x0 + c - 2, y0 + c - 2, x0 + c + 2, y0 + c + 2, 0xFFFFFFFF);
        ctx.fill(x0 + c - 1, y0 + c - 1, x0 + c + 1, y0 + c + 1, 0xFFE5484D);
        ctx.drawTextWithShadow(client.textRenderer, "N", x0 + size / 2 - 2, y0 + 1, 0xFFFFFFFF);
    }
}
