package net.prismfeatures.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.prismfeatures.PrismFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vision des shulkers : survoler un shulker dans un inventaire affiche son contenu. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow
    protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void prism$shulkerPreview(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.focusedSlot != null) PrismFeatures.onSlotHover(context, this.focusedSlot, mouseX, mouseY);
    }
}
