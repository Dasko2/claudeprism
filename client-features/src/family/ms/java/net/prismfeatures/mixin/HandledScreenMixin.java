package net.prismfeatures.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.prismfeatures.Gfx;
import net.prismfeatures.PrismFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow
    protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void prism$shulkerPreview(MatrixStack context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.focusedSlot != null) PrismFeatures.onSlotHover(new Gfx(context), this.focusedSlot, mouseX, mouseY);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), require = 0)
    private void prism$shulkerPreview2(MatrixStack context, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.focusedSlot != null) PrismFeatures.onSlotHover(new Gfx(context), this.focusedSlot, mouseX, mouseY);
    }
}
