package com.xfastgames.witness.blocks.flowers

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class JasmineBush : FlowerBush() {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "jasmine_bush")
        val BLOCK = registerBlock(JasmineBush(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

}