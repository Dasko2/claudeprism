package net.prismfeatures.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import net.prismfeatures.PrismFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class InGameHudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void prism$render(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        PrismFeatures.onHud(context);
    }
}
