package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fonctionnalités intégrées. Aucune dépendance à Fabric API : tout passe par des mixins.
 * Chaque point d'entrée est protégé : une erreur ne doit jamais faire planter le jeu.
 */
public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    private static boolean nightVisionOn = false;
    private static boolean nightKeyWasDown = false;
    private static boolean failed = false;

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
        LOGGER.info("[PrismFeatures] chargé (nightVision={}, minimap={}, coffres={}, shulker={})",
                Config.nightVision, Config.minimap, Config.chestPreview, Config.shulkerPreview);
    }

    // ---------------------------------------------------------------- tick
    public static void onTick(MinecraftClient client) {
        if (failed) return;
        try {
            if (client.player == null || client.world == null) {
                nightVisionOn = false;
                return;
            }

            // N : basculer la night vision (seulement si aucun écran n'est ouvert)
            boolean down = client.currentScreen == null && InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_N);
            if (down && !nightKeyWasDown && Config.nightVision) {
                nightVisionOn = !nightVisionOn;
                client.player.sendMessage(Text.literal("Night vision : " + (nightVisionOn ? "activée" : "désactivée")), true);
                if (!nightVisionOn) client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            nightKeyWasDown = down;
            if (!Config.nightVision && nightVisionOn) {
                nightVisionOn = false;
                client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            if (nightVisionOn && !client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                // effet purement local, infini, sans particules ni icône
                client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false, false));
            }

            // coffres : mémoriser le contenu tant qu'un conteneur est ouvert
            if (Config.chestPreview && lastClicked != null && client.currentScreen instanceof HandledScreen<?> screen) {
                ScreenHandler handler = screen.getScreenHandler();
                int size = -1;
                if (handler instanceof GenericContainerScreenHandler g) size = g.getRows() * 9;
                else if (handler instanceof ShulkerBoxScreenHandler) size = 27;
                if (size > 0) {
                    List<ItemStack> copy = new ArrayList<>();
                    for (int i = 0; i < size && i < handler.slots.size(); i++) {
                        ItemStack s = handler.slots.get(i).getStack();
                        if (!s.isEmpty()) copy.add(s.copy());
                    }
                    chestMemory.put(lastClicked, copy);
                }
            }

            // minimap : recalcul toutes les 10 ticks
            if (Config.minimap && (mapTick++ % 10 == 0)) rebuildMap(client);
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[PrismFeatures] erreur dans le tick, fonctionnalités désactivées pour cette session", t);
        }
    }

    public static void onBlockUsed(BlockPos pos) {
        try {
            lastClicked = pos.toImmutable();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- shulker
    public static void addShulkerTooltip(ItemStack stack, List<Text> lines) {
        try {
            if (!Config.shulkerPreview || !isShulker(stack)) return;
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            container.streamNonEmpty().forEach(items::add);
            if (items.isEmpty()) return;
            lines.add(Text.literal("Contenu :").formatted(Formatting.GOLD));
            lines.addAll(summarize(items, 10));
        } catch (Throwable t) {
            LOGGER.warn("[PrismFeatures] aperçu shulker : {}", t.toString());
        }
    }

    // ---------------------------------------------------------------- HUD
    public static void onHud(DrawContext ctx) {
        if (failed) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.options.hudHidden) return;

            if (Config.hud) {
                BlockPos p = client.player.getBlockPos();
                ctx.drawTextWithShadow(client.textRenderer, client.getCurrentFps() + " FPS", 4, 4, 0xFFFFFFFF);
                ctx.drawTextWithShadow(client.textRenderer, "XYZ " + p.getX() + " " + p.getY() + " " + p.getZ(), 4, 15, 0xFFFFFFFF);
            }

            if (Config.minimap && mapReady) drawMap(ctx, client);

            if (Config.chestPreview && client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult hit
                    && hit.getType() == HitResult.Type.BLOCK) {
                List<ItemStack> items = chestMemory.get(hit.getBlockPos());
                if (items != null) {
                    int x = ctx.getScaledWindowWidth() / 2 + 12;
                    int y = ctx.getScaledWindowHeight() / 2 + 12;
                    if (items.isEmpty()) {
                        ctx.drawTextWithShadow(client.textRenderer, Text.literal("Vide").formatted(Formatting.GRAY), x, y, 0xFFFFFFFF);
                    } else {
                        for (Text line : summarize(items, 8)) {
                            ctx.drawTextWithShadow(client.textRenderer, line, x, y, 0xFFFFFFFF);
                            y += 10;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[PrismFeatures] erreur d'affichage, fonctionnalités désactivées pour cette session", t);
        }
    }

    // ---------------------------------------------------------------- minimap
    private static void rebuildMap(MinecraftClient client) {
        World world = client.world;
        BlockPos center = client.player.getBlockPos();
        int half = MAP_CELLS / 2;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int cz = 0; cz < MAP_CELLS; cz++) {
            for (int cx = 0; cx < MAP_CELLS; cx++) {
                int wx = center.getX() - half + cx;
                int wz = center.getZ() - half + cz;
                int color = 0xFF101018; // zone non chargée
                if (world.isChunkLoaded(wx >> 4, wz >> 4)) {
                    int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                    pos.set(wx, y, wz);
                    int rgb = world.getBlockState(pos).getMapColor(world, pos).color;
                    // relief : plus clair si le voisin nord est plus bas, plus sombre s'il est plus haut
                    int yn = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz - 1) - 1;
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
        // joueur au centre
        int c = size / 2;
        ctx.fill(x0 + c - 2, y0 + c - 2, x0 + c + 2, y0 + c + 2, 0xFFFFFFFF);
        ctx.fill(x0 + c - 1, y0 + c - 1, x0 + c + 1, y0 + c + 1, 0xFFE5484D);
        ctx.drawTextWithShadow(client.textRenderer, "N", x0 + size / 2 - 2, y0 + 1, 0xFFFFFFFF);
    }

    // ---------------------------------------------------------------- outils
    private static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    /** « Diamant x12 » par type d'objet, limité à max lignes. */
    private static List<Text> summarize(List<ItemStack> stacks, int max) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (ItemStack s : stacks) counts.merge(s.getItem(), s.getCount(), Integer::sum);
        List<Text> out = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<Item, Integer> e : counts.entrySet()) {
            if (shown++ >= max) {
                out.add(Text.literal("… +" + (counts.size() - max) + " autres").formatted(Formatting.GRAY));
                break;
            }
            out.add(Text.literal("  ").append(e.getKey().getName()).append(Text.literal(" x" + e.getValue()).formatted(Formatting.YELLOW)));
        }
        return out;
    }
}
