package com.xfastgames.witness.blocks.yucca

import com.xfastgames.witness.WITNESS_ID
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.BlockState
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*

class TallYucca : Yucca() {

    companion object {
        val IDENTIFIER = Identifier(WITNESS_ID, "tall_yucca")
        val BLOCK = registerBlock(TallYucca(), IDENTIFIER, RenderLayer.getCutout())
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER, RenderLayer.getCutout())
    }

    override fun grow(world: ServerWorld, random: Random?, pos: BlockPos, state: BlockState) {
        dropStack(world, pos, ItemStack(Yucca.BLOCK))
    }
}