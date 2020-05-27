package com.xfastgames.witness.blocks.drapes

import net.minecraft.block.Block

class BlueBougainvilleaDrape : Drape() {
    companion object {
        val BLOCK by lazy { BlueBougainvilleaDrape() }
    }

    override fun isDrape(block: Block) = block is BlueBougainvilleaDrape
}