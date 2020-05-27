package com.xfastgames.witness.blocks.drapes

import net.minecraft.block.Block

class PurpleBougainvilleaDrape : Drape() {
    companion object {
        val BLOCK by lazy { PurpleBougainvilleaDrape() }
    }

    override fun isDrape(block: Block) = block is PurpleBougainvilleaDrape
}