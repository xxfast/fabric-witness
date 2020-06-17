package com.xfastgames.witness.blocks.drapes

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class PurpleBougainvilleaDrape : Drape(), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "blue_bougainvillea")
        val BLOCK = registerBlock(PurpleBougainvilleaDrape(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, Item.Settings().group(ItemGroup.DECORATIONS))
    }

    override fun isDrape(block: Block) = block is PurpleBougainvilleaDrape

    override fun onClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
    }
}