package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.blockSettings
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.VineBlock
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.world.level.block.SoundType
import net.minecraft.resources.Identifier

class OakLeavesRunners(settings: BlockBehaviour.Properties) : VineBlock(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "oak_leaves_runners")
        val BLOCK = registerBlock(
            OakLeavesRunners(
                blockSettings(IDENTIFIER)
                    .noCollision()
                    .randomTicks()
                    .strength(0.2f)
                    .sound(SoundType.GRASS)
            ),
            IDENTIFIER
        )
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun onClient() {
        // Fixed foliage tint. Item tints are data-driven via the item model (no ITEM colour registry).
        // BlockRenderLayerMap was removed in Fabric 26.x; translucent layer must come from the model.
        // Tints are ARGB in 26.x and multiplied into the quad alpha as well, so the alpha byte must
        // be set: a bare 0xRRGGBB has alpha 0 and renders the block fully transparent.
        BlockColorRegistry.register(listOf(BlockTintSources.constant(0xFFA0AB42.toInt())), BLOCK)
    }
}
