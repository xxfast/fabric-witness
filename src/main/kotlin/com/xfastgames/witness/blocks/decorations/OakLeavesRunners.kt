package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.VineBlock
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier

class OakLeavesRunners(settings: AbstractBlock.Settings) : VineBlock(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "oak_leaves_runners")
        val BLOCK = registerBlock(
            OakLeavesRunners(
                blockSettings(IDENTIFIER)
                    .noCollision()
                    .ticksRandomly()
                    .strength(0.2f)
                    .sounds(BlockSoundGroup.GRASS)
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun onClient() {
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { _, _, _, _ -> 0xA0AB42 }, BLOCK)
        // NOTE(migration): ColorProviderRegistry.ITEM was removed; item tints are now driven by the
        // item model's `tintindex`/tint sources (data-driven). The item colour is therefore dropped here.
        BlockRenderLayerMap.putBlock(BLOCK, BlockRenderLayer.TRANSLUCENT)
    }
}
