package com.xfastgames.witness.blocks.drapes

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.Block
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class PurpleBougainvilleaDrape : Drape() {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "blue_bougainvillea")
        val BLOCK = registerBlock(PurpleBougainvilleaDrape(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

    override fun isDrape(block: Block) = block is PurpleBougainvilleaDrape
}