package net.prismfeatures.mixin;

import net.minecraft.client.render.Camera;
import net.prismfeatures.Freelook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreelookCameraMixin {
    @Shadow private float yaw;
    @Shadow private float pitch;
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void prism$freelookCamera(CallbackInfo ci) {
        if (!Freelook.active) return;
        float p = Math.max(-90f, Math.min(90f, this.pitch + Freelook.dpitch));
        this.setRotation(this.yaw + Freelook.dyaw, p);
    }
}
