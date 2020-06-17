package com.xfastgames.witness.blocks.flowers

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class JasmineBush : FlowerBush(), Clientside {

    companion object {
        val IDENTIFIER = Identifier(Witness.IDENTIFIER, "jasmine_bush")
        val BLOCK = registerBlock(JasmineBush(), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun onClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout())
    }

}