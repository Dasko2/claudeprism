package net.prismfeatures.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Permet d'écrire une valeur hors limites (gamma) sans passer par la validation du slider. */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
    @Mutable
    @Accessor("value")
    void prism$setValueUnchecked(T value);
}
