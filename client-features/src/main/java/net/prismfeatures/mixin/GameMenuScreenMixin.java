package net.prismfeatures.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.prismfeatures.FeaturesScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ajoute le bouton « Menu de config » dans le menu Échap. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void prism$addConfigButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Menu de config"),
                b -> this.client.setScreen(new FeaturesScreen(this))).dimensions(6, 6, 110, 20).build());
    }
}
