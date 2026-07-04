package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.PillarBlock
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

class CedarLog(settings: AbstractBlock.Settings) : PillarBlock(settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "cedar_log")
        val BLOCK = registerBlock(
            CedarLog(blockSettings(IDENTIFIER).strength(2.0f).sounds(BlockSoundGroup.WOOD)),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

}
