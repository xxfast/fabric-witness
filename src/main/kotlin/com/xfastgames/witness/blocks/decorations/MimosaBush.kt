package com.xfastgames.witness.blocks.decorations

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.block.AbstractBlock
import net.minecraft.block.PlantBlock
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.util.Identifier

class MimosaBush(settings: AbstractBlock.Settings) : FlowerBush(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "mimosa_bush")
        val CODEC: MapCodec<MimosaBush> = createCodec(::MimosaBush)
        val BLOCK = registerBlock(MimosaBush(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun getCodec(): MapCodec<out PlantBlock> = CODEC

    override fun onClient() {
        BlockRenderLayerMap.putBlock(BLOCK, BlockRenderLayer.CUTOUT)
    }

}
