package com.xfastgames.witness.mixin.utils;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseAccessorMixin {

    @Accessor("mouseGrabbed")
    boolean getCursorLocked();

    @Accessor("mouseGrabbed")
    void setCursorLocked(boolean locked);
}
