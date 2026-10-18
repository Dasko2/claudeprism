package net.prismfeatures;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.prismfeatures.mixin.SimpleOptionAccessor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrismFeatures implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("prismfeatures");

    private static final double FULLBRIGHT_GAMMA = 16.0;

    private KeyBinding nightVisionKey;
    private boolean nightVisionOn = false;
    private double savedGamma = 1.0;

    /** Dernier bloc cliqué (clic droit) et contenu mémorisé des conteneurs déjà ouverts. */
    private BlockPos lastClicked = null;
    private final Map<BlockPos, List<ItemStack>> chestMemory = new HashMap<>();

    @Override
    public void onInitializeClient() {
        Config.load();

        nightVisionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prismfeatures.nightvision", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.prismfeatures"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> setNightVision(client, false));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> chestMemory.clear());

        // 1) contenu des shulker boxes dans l'infobulle
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!Config.shulkerPreview || !isShulker(stack)) return;
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null) return;
            List<ItemStack> items = new ArrayList<>();
            container.streamNonEmpty().forEach(items::add);
            if (items.isEmpty()) return;
            lines.add(Text.literal("Contenu :").formatted(Formatting.GOLD));
            lines.addAll(summarize(items, 10));
        });

        // 2) mémoriser le bloc sur lequel on fait clic droit
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) lastClicked = hit.getBlockPos().toImmutable();
            return ActionResult.PASS;
        });

        // 3) affichage : contenu mémorisé du coffre visé + FPS / coordonnées
        HudRenderCallback.EVENT.register(this::onHud);
    }

    private void onTick(MinecraftClient client) {
        while (nightVisionKey.wasPressed()) {
            if (Config.nightVision) setNightVision(client, !nightVisionOn);
        }
        if (!Config.nightVision && nightVisionOn) setNightVision(client, false);

        // mémorise le contenu tant qu'un conteneur est ouvert
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
    }

    @SuppressWarnings("unchecked")
    private void setNightVision(MinecraftClient client, boolean on) {
        if (client == null || client.options == null || on == nightVisionOn) return;
        SimpleOption<Double> gamma = client.options.getGamma();
        if (on) {
            savedGamma = gamma.getValue();
            ((SimpleOptionAccessor<Double>) (Object) gamma).prism$setValueUnchecked(FULLBRIGHT_GAMMA);
        } else {
            ((SimpleOptionAccessor<Double>) (Object) gamma).prism$setValueUnchecked(savedGamma);
        }
        nightVisionOn = on;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Night vision : " + (on ? "activée" : "désactivée")), true);
        }
    }

    private void onHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) return;

        if (Config.hud) {
            BlockPos p = client.player.getBlockPos();
            ctx.drawTextWithShadow(client.textRenderer, client.getCurrentFps() + " FPS", 4, 4, 0xFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer, "XYZ " + p.getX() + " " + p.getY() + " " + p.getZ(), 4, 15, 0xFFFFFF);
        }

        if (Config.chestPreview && client.currentScreen == null && client.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            List<ItemStack> items = chestMemory.get(hit.getBlockPos());
            if (items != null) {
                int x = ctx.getScaledWindowWidth() / 2 + 12;
                int y = ctx.getScaledWindowHeight() / 2 + 12;
                if (items.isEmpty()) {
                    ctx.drawTextWithShadow(client.textRenderer, Text.literal("Vide").formatted(Formatting.GRAY), x, y, 0xFFFFFF);
                } else {
                    for (Text line : summarize(items, 8)) {
                        ctx.drawTextWithShadow(client.textRenderer, line, x, y, 0xFFFFFF);
                        y += 10;
                    }
                }
            }
        }
    }

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
