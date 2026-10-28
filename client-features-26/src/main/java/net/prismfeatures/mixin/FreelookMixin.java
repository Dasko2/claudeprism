package net.prismfeatures.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.prismfeatures.Freelook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FreelookMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void prism$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.active && (Object) this == Minecraft.getInstance().player) {
            Freelook.look(dx, dy);
            ci.cancel();
        }
    }
}
