package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.resources.Identifier
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level

class PinkCedarLeaves(settings: BlockBehaviour.Properties) : LeavesBlock(0.01f, settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "pink_cedar_leaves")
        val CODEC: MapCodec<PinkCedarLeaves> = simpleCodec(::PinkCedarLeaves)
        val BLOCK = registerBlock(
            PinkCedarLeaves(
                blockSettings(IDENTIFIER)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun codec(): MapCodec<out LeavesBlock> = CODEC

    // Pink cedar leaves have no falling-leaf particle (matches pre-1.21 behaviour).
    override fun spawnFallingLeavesParticle(world: Level, pos: BlockPos, random: RandomSource) = Unit

    override fun onClient() {
    }
}
