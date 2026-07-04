package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockState
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random

class TallYucca(settings: AbstractBlock.Settings) : Yucca(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "tall_yucca")
        val BLOCK = registerBlock(TallYucca(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun grow(world: ServerWorld, random: Random?, pos: BlockPos, state: BlockState) {
        dropStack(world, pos, ItemStack(Yucca.BLOCK))
    }

    override fun onClient() {
        BlockRenderLayerMap.putBlock(BLOCK, BlockRenderLayer.CUTOUT)
    }
}
