package net.prismfeatures.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.prismfeatures.PrismFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vision des shulkers : survoler un shulker dans un inventaire affiche son contenu. */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen {
    @Shadow
    protected Slot hoveredSlot;

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void prism$shulkerPreview(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.hoveredSlot != null) PrismFeatures.onSlotHover(context, this.hoveredSlot, mouseX, mouseY);
    }
}
