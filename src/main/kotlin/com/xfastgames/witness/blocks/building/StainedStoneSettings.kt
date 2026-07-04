package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.utils.blockSettings
import net.minecraft.block.AbstractBlock
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

/**
 * Builds fresh stained-stone block settings for the given registration [id].
 * (1.21.2+ requires each block's settings to carry its own registry key, so this is a
 * factory rather than a shared value.) [net.minecraft.block.Material] and the Fabric
 * `breakByTool`/`breakByHand` helpers were removed; mining tool requirements are now
 * data-driven via block tags.
 */
fun stainedStoneSettings(id: Identifier): AbstractBlock.Settings =
    blockSettings(id)
        .sounds(BlockSoundGroup.STONE)
        .strength(1.5f, 6f)
