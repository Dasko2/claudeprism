package net.prismfeatures.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.prismfeatures.Extras;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void prism$hideVanillaCrosshair(CallbackInfo ci) {
        if (Extras.customCrosshair()) ci.cancel();
    }
}
