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
                  identifier: Identifier = Identifier(WITNESS_ID, name),
                  settings: Item.Settings = Item.Settings().group(ItemGroup.MISC)
) {
    Registry.register(Registry.BLOCK, identifier, block)
    Registry.register(Registry.ITEM, identifier, BlockItem(block, settings))
}