package com.xfastgames.witness.utils

import com.xfastgames.witness.WITNESS_ID
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun registerBlock(block: Block,
                  name: String,
                  settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
) {
    val id = Identifier(WITNESS_ID, name)
    Registry.register(Registry.BLOCK, id, block)
    Registry.register(Registry.ITEM, id, BlockItem(block, settings))
}