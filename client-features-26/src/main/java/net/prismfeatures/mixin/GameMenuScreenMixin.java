package net.prismfeatures.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.prismfeatures.Compat;
import net.prismfeatures.FeaturesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void prism$addConfigButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal(T.t("Menu de config")),
                b -> Compat.setScreen(this.minecraft, new FeaturesScreen(this))).bounds(6, 6, 110, 20).build());
    }
}
