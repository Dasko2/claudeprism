package net.prismfeatures.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.prismfeatures.Freelook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook : la souris déplace la caméra au lieu du joueur. */
@Mixin(Entity.class)
public abstract class FreelookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void prism$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.active && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.look(dx, dy);
            ci.cancel();
        }
    }
}
