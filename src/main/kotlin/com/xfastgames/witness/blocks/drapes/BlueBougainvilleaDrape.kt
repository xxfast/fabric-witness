package com.xfastgames.witness.blocks.drapes

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class BlueBougainvilleaDrape : Drape() {

    companion object {
        val IDENTIFIER = Identifier(WITNESS_ID, "purple_bougainvillea")
        val BLOCK = registerBlock(BlueBougainvilleaDrape(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

    override fun isDrape(block: Block) = block is BlueBougainvilleaDrape
}