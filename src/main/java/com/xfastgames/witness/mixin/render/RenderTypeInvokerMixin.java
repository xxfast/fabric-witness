package com.xfastgames.witness.mixin.render;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code RenderType.create} is package-private (since 26.2) and Fabric API has no helper for it; the
 * screen frame's CRT layer needs it to put its own pipeline behind a render type.
 */
@Mixin(RenderType.class)
public interface RenderTypeInvokerMixin {

    @Invoker("create")
    static RenderType invokeCreate(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
