package com.xfastgames.witness.mixin.utils;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessorMixin {

    @Accessor("cursorLocked")
    boolean getCursorLocked();

    @Accessor("cursorLocked")
    void setCursorLocked(boolean locked);
}
