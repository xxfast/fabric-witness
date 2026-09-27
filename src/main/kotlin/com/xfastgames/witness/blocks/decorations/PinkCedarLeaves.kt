package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer
import net.minecraft.resources.Identifier

class PinkCedarLeaves(settings: BlockBehaviour.Properties) : LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "pink_cedar_leaves")
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

    override fun onClient() {
    }
}
