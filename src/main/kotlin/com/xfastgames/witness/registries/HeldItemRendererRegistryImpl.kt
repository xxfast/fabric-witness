package com.xfastgames.witness.registries

import net.minecraft.client.render.item.HeldItemRenderer
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry
import java.util.*
import kotlin.collections.HashMap

class HeldItemRendererRegistryImpl private constructor() : HeldItemRendererRegistry {
    override fun register(item: Item, renderer: HeldItemRenderer) {
        Objects.requireNonNull(item, "item is null")
        Objects.requireNonNull(renderer, "renderer is null")
        require(!RENDERERS.containsKey(item)) { "Item " + Registry.ITEM.getId(item) + " already has a HeldItemRenderer!" }
        RENDERERS[item] = renderer
    }

    companion object {
        val INSTANCE = HeldItemRendererRegistryImpl()
        private val RENDERERS: MutableMap<Item, HeldItemRenderer?> = HashMap()
        fun getRenderer(item: Item): HeldItemRenderer? = RENDERERS[item]
    }
}