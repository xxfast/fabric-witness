package com.xfastgames.witness.blocks.decorations

import com.mojang.serialization.MapCodec
import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.Clientside
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.VegetationBlock
import net.minecraft.world.level.block.Block
import net.minecraft.resources.Identifier

class BlueBougainvilleaDrape(settings: BlockBehaviour.Properties) : Drape(settings), Clientside {

    companion object {
        val IDENTIFIER = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "purple_bougainvillea")
        val CODEC: MapCodec<BlueBougainvilleaDrape> = simpleCodec(::BlueBougainvilleaDrape)
        val BLOCK = registerBlock(BlueBougainvilleaDrape(drapeSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun isDrape(block: Block) = block is BlueBougainvilleaDrape

    override fun codec(): MapCodec<out VegetationBlock> = CODEC

    override fun onClient() {
    }
}
