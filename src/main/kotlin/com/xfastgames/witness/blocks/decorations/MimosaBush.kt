package com.xfastgames.witness.blocks.decorations

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.resources.Identifier

class MimosaBush(settings: BlockBehaviour.Properties) : FlowerBush(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "mimosa_bush")
        val BLOCK = registerBlock(MimosaBush(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun onClient() {
    }

}
