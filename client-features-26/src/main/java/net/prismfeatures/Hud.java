package net.prismfeatures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Éléments d'affichage déplaçables (infos, armure, main, seconde main, point de mort). */
public final class Hud {
    private Hud() {}

    public abstract static class Element {
        public final String id;
        public final String label;
        final String toggleKey;
        final double defX;
        final double defY;

        Element(String id, String label, String toggleKey, double defX, double defY) {
            this.id = id;
            this.label = label;
            this.toggleKey = toggleKey;
            this.defX = defX;
            this.defY = defY;
        }

        abstract int width(Minecraft c, boolean preview);
        abstract int height(Minecraft c, boolean preview);
        abstract void draw(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview);
        abstract boolean hasContent(Minecraft c);

        public double scale() { return Config.scale(id); }
        int sw(Minecraft c, boolean preview) { return (int) Math.ceil(width(c, preview) * scale()); }
        int sh(Minecraft c, boolean preview) { return (int) Math.ceil(height(c, preview) * scale()); }

        public int x(int screenW, Minecraft c, boolean preview) {
            return (int) Math.round(Config.pos(id, defX, defY)[0] * Math.max(0, screenW - sw(c, preview)));
        }

        public int y(int screenH, Minecraft c, boolean preview) {
            return (int) Math.round(Config.pos(id, defX, defY)[1] * Math.max(0, screenH - sh(c, preview)));
        }

        public int w(Minecraft c) { return sw(c, true); }
        public int h(Minecraft c) { return sh(c, true); }

        /** Dessine l'élément à sa taille choisie (échelle). */
        public void drawScaled(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview) {
            double s = scale();
            if (s == 1.0) {
                draw(ctx, c, x, y, preview);
                return;
            }
            Compat.push(ctx, x, y, (float) s);
            draw(ctx, c, 0, 0, preview);
            Compat.pop(ctx);
        }

        public void drawPreview(GuiGraphics ctx, Minecraft c, int x, int y) { drawScaled(ctx, c, x, y, true); }
    }

    private static ItemStack damaged(net.minecraft.world.item.Item item, int damage) {
        ItemStack s = new ItemStack(item);
        s.setDamageValue(damage);
        return s;
    }

    private static int durabilityColor(ItemStack s) {
        if (!s.isDamageableItem()) return 0xFFFFFFFF;
        float f = (float) (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage();
        if (f > 0.5f) return 0xFF55FF55;
        if (f > 0.2f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    /** Durabilité restante pour un objet qui s'use, sinon la quantité. */
    private static String number(ItemStack s) {
        return s.isDamageableItem() ? String.valueOf(s.getMaxDamage() - s.getDamageValue()) : String.valueOf(s.getCount());
    }

    /** Ligne « image de l'objet + nombre à côté ». */
    private static void drawItemRow(GuiGraphics ctx, Minecraft c, ItemStack s, int x, int y) {
        ctx.renderItem(s, x, y);
        ctx.drawString(c.font, number(s), x + 22, y + 5, durabilityColor(s));
    }

    // ---------------------------------------------------------------- infos
    private static final Element INFOS = new Element("infos", "Infos", Config.HUD, 0.0, 0.0) {
        private List<String> lines(Minecraft c) {
            Player p = c.player;
            List<String> out = new ArrayList<>();
            out.add(c.getFps() + " FPS");
            if (p == null || c.level == null) return out;
            BlockPos pos = p.blockPosition();
            String dir = switch (p.getDirection()) {
                case NORTH -> "N";
                case SOUTH -> "S";
                case EAST -> "E";
                default -> "O";
            };
            out.add("XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  " + dir);
            out.add("Biome : " + c.level.getBiome(pos).unwrapKey().map(k -> k.identifier().getPath().replace('_', ' ')).orElse("?"));
            if (c.getConnection() != null && c.getServer() == null) {
                var entry = c.getConnection().getPlayerInfo(p.getUUID());
                if (entry != null) out.add("Ping : " + entry.getLatency() + " ms");
            }
            out.add("Heure : " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            return out;
        }

        int width(Minecraft c, boolean preview) {
            int w = 40;
            for (String l : lines(c)) w = Math.max(w, c.font.width(l));
            return w;
        }

        int height(Minecraft c, boolean preview) { return lines(c).size() * 11; }
        boolean hasContent(Minecraft c) { return true; }

        void draw(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview) {
            for (String l : lines(c)) {
                ctx.drawString(c.font, l, x, y, 0xFFFFFFFF);
                y += 11;
            }
        }
    };

    // ---------------------------------------------------------------- armure
    private static final Element ARMOR = new Element("armor", "Armure", Config.ARMOR, 0.0, 0.45) {
        private final EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

        private List<ItemStack> pieces(Minecraft c, boolean preview) {
            List<ItemStack> out = new ArrayList<>();
            for (EquipmentSlot slot : slots) {
                ItemStack s = c.player == null ? ItemStack.EMPTY : c.player.getItemBySlot(slot);
                if (!s.isEmpty()) out.add(s);
            }
            if (out.isEmpty() && preview) {
                out.add(damaged(Items.DIAMOND_HELMET, 40));
                out.add(damaged(Items.DIAMOND_CHESTPLATE, 120));
                out.add(damaged(Items.DIAMOND_LEGGINGS, 200));
                out.add(damaged(Items.DIAMOND_BOOTS, 300));
            }
            return out;
        }

        int width(Minecraft c, boolean preview) { return 22 + 36; }
        int height(Minecraft c, boolean preview) { return Math.max(1, pieces(c, preview).size()) * 18; }
        boolean hasContent(Minecraft c) { return !pieces(c, false).isEmpty(); }

        void draw(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview) {
            for (ItemStack s : pieces(c, preview)) {
                drawItemRow(ctx, c, s, x, y);
                y += 18;
            }
        }
    };

    // ---------------------------------------------------------------- mains
    private static final class HandElement extends Element {
        private final boolean off;

        HandElement(String id, String label, String toggle, double dx, double dy, boolean off) {
            super(id, label, toggle, dx, dy);
            this.off = off;
        }

        private ItemStack stack(Minecraft c, boolean preview) {
            ItemStack s = c.player == null ? ItemStack.EMPTY : (off ? c.player.getOffhandItem() : c.player.getMainHandItem());
            if (s.isEmpty() && preview) return off ? new ItemStack(Items.SHIELD) : damaged(Items.DIAMOND_PICKAXE, 250);
            return s;
        }

        int width(Minecraft c, boolean preview) { return 22 + 36; }
        int height(Minecraft c, boolean preview) { return 18; }
        boolean hasContent(Minecraft c) { return !stack(c, false).isEmpty(); }

        void draw(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview) {
            ItemStack s = stack(c, preview);
            if (s.isEmpty()) return;
            // outil : durabilité restante ; sinon : nombre TOTAL dans l'inventaire (shulkers compris)
            String text;
            if (s.isDamageableItem()) text = String.valueOf(s.getMaxDamage() - s.getDamageValue());
            else text = String.valueOf(preview && c.player == null ? s.getCount() : PrismFeatures.countInInventory(c.player, s));
            ctx.renderItem(s, x, y);
            ctx.drawString(c.font, text, x + 22, y + 5, durabilityColor(s));
        }
    }

    private static final Element MAIN = new HandElement("mainHand", "Objet en main", Config.MAIN_HAND, 0.0, 0.62, false);
    private static final Element OFF = new HandElement("offHand", "Seconde main", Config.OFF_HAND, 0.0, 0.70, true);

    // ---------------------------------------------------------------- mort
    private static final Element DEATH = new Element("death", "Point de mort", Config.DEATH, 0.0, 0.2) {
        private String text() {
            BlockPos d = PrismFeatures.lastDeath;
            return d == null ? "Mort : 120 64 -300" : "Mort : " + d.getX() + " " + d.getY() + " " + d.getZ();
        }

        int width(Minecraft c, boolean preview) { return c.font.width(text()); }
        int height(Minecraft c, boolean preview) { return 10; }
        boolean hasContent(Minecraft c) { return PrismFeatures.lastDeath != null; }

        void draw(GuiGraphics ctx, Minecraft c, int x, int y, boolean preview) {
            ctx.drawString(c.font, text(), x, y, 0xFFFF5555);
        }
    };

    public static final List<Element> ELEMENTS = List.of(INFOS, ARMOR, MAIN, OFF, DEATH);

    /** Affichage en jeu. */
    public static void render(GuiGraphics ctx, Minecraft c) {
        int w = ctx.guiWidth();
        int h = ctx.guiHeight();
        for (Element e : ELEMENTS) {
            final Element el = e;
            if (!Config.enabled(el.toggleKey)) continue;
            PrismFeatures.safe("affichage-" + el.id, () -> {
                if (el.hasContent(c)) el.drawScaled(ctx, c, el.x(w, c, false), el.y(h, c, false), false);
            });
        }
    }
}
