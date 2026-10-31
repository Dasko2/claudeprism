package net.prismfeatures.mixin;

import net.minecraft.component.type.ContainerComponent;
import net.prismfeatures.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerComponent.class)
public class ContainerComponentMixin {
    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void prism$hideVanilla(CallbackInfo ci) {
        if (Config.enabled(Config.SHULKER)) ci.cancel();
    }
}
