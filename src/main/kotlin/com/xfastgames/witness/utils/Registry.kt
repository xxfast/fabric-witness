package com.xfastgames.witness.utils

import com.xfastgames.witness.WITNESS_ID
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun registerBlock(block: Block,
                  name: String,
                  transparent: Boolean = false,
                  settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
) {
    val id = Identifier(WITNESS_ID, name)
    Registry.register(Registry.BLOCK, id, block)
    Registry.register(Registry.ITEM, id, BlockItem(block, settings))
    if (transparent) BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent())
}