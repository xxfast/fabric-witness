package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.mojang.serialization.MapCodec
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.block.AbstractBlock
import net.minecraft.block.LeavesBlock
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.World

class PinkCedarLeaves(settings: AbstractBlock.Settings) : LeavesBlock(0.01f, settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "pink_cedar_leaves")
        val CODEC: MapCodec<PinkCedarLeaves> = createCodec(::PinkCedarLeaves)
        val BLOCK = registerBlock(
            PinkCedarLeaves(
                blockSettings(IDENTIFIER)
                    .strength(0.2F)
                    .ticksRandomly()
                    .sounds(BlockSoundGroup.GRASS)
                    .nonOpaque()
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun getCodec(): MapCodec<out LeavesBlock> = CODEC

    // Pink cedar leaves have no falling-leaf particle (matches pre-1.21 behaviour).
    override fun spawnLeafParticle(world: World?, pos: BlockPos?, random: Random?) = Unit

    override fun onClient() {
        BlockRenderLayerMap.putBlock(BLOCK, BlockRenderLayer.CUTOUT)
    }
}
