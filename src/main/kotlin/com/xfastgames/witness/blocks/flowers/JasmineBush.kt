package com.xfastgames.witness.blocks.flowers

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class JasmineBush : FlowerBush() {

    companion object {
        val IDENTIFIER = Identifier(WITNESS_ID, "jasmine_bush")
        val BLOCK = registerBlock(JasmineBush(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

}