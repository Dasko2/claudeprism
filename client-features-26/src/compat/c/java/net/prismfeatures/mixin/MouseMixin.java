package net.prismfeatures.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.prismfeatures.Env;
import net.prismfeatures.FeaturesScreen;
import net.prismfeatures.HudEditorScreen;
import net.prismfeatures.PrismFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), require = 0)
    private void prism$mouse(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        var screen = mc.gui.screen();
        PrismFeatures.LOGGER.info("[PrismFeatures] souris bouton={} action={} ecran={}", info.button(), action, screen == null ? "aucun" : screen.getClass().getSimpleName());
        if (!(screen instanceof FeaturesScreen) && !(screen instanceof HudEditorScreen)) return;
        if (info.button() != 0) return;
        MouseHandler self = (MouseHandler) (Object) this;
        if (action == 1) {
            Env.leftHeld = true;
            Env.clickPending = true;
            Env.clickX = self.getScaledXPos(mc.getWindow());
            Env.clickY = self.getScaledYPos(mc.getWindow());
        } else if (action == 0) {
            Env.leftHeld = false;
        }
    }
}
