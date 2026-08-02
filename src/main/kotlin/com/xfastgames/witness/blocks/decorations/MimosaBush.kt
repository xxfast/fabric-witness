package com.xfastgames.witness.blocks.decorations

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.VegetationBlock
import net.minecraft.resources.Identifier

class MimosaBush(settings: BlockBehaviour.Properties) : FlowerBush(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "mimosa_bush")
        val CODEC: MapCodec<MimosaBush> = simpleCodec(::MimosaBush)
        val BLOCK = registerBlock(MimosaBush(bushSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun codec(): MapCodec<out VegetationBlock> = CODEC

    override fun onClient() {
    }

}
