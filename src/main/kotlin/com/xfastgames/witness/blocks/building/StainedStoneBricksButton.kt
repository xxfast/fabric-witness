package com.xfastgames.witness.blocks.building

import com.xfastgames.witness.Witness
import com.xfastgames.witness.utils.registerBlock
import com.xfastgames.witness.utils.registerBlockItem
import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockSetType
import net.minecraft.block.ButtonBlock
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

class StainedStoneBricksButton(settings: AbstractBlock.Settings) :
    ButtonBlock(BlockSetType.STONE, 20, settings) {

    companion object {
        val IDENTIFIER = Identifier.of(Witness.IDENTIFIER, "yellow_stained_stone_bricks_button")
        val BLOCK = registerBlock(StainedStoneBricksButton(stainedStoneSettings(IDENTIFIER)), IDENTIFIER)
        val BLOCK_ITEM = registerBlockItem(BLOCK, IDENTIFIER)
    }

    override fun getClickSound(powered: Boolean): SoundEvent =
        if (powered) SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON
        else SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF
}
