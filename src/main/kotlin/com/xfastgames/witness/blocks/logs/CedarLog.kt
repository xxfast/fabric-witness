package com.xfastgames.witness.blocks.logs

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.LogBlock
import net.minecraft.block.Material
import net.minecraft.block.MaterialColor
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

class CedarLog : LogBlock(
    MaterialColor.WOOD,
    FabricBlockSettings.of(Material.WOOD, MaterialColor.SPRUCE).strength(2.0f)
        .sounds(BlockSoundGroup.WOOD)
) {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "cedar_log")
        val BLOCK = registerBlock(CedarLog(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.BUILDING_BLOCKS))
    }

}