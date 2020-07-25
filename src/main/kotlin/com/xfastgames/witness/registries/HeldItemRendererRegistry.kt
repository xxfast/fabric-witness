package com.xfastgames.witness.registries

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.item.Item


/**
 * This registry holds [held item renderers][HeldItemRenderer] for items.
 */
@Environment(EnvType.CLIENT)
interface HeldItemRendererRegistry {
    fun register(item: Item, renderer: HeldItemRenderer)

    companion object {
        val INSTANCE: HeldItemRendererRegistry = HeldItemRendererRegistryImpl.INSTANCE
    }
}
