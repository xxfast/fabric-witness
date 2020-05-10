package com.xfastgames.witness.blocks.stained.stone.bricks

import com.xfastgames.witness.blocks.stained.stone.stainedStoneSettings
import net.minecraft.block.AbstractButtonBlock
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents

class StainedStoneButton : AbstractButtonBlock(false, stainedStoneSettings) {
    override fun getClickSound(powered: Boolean): SoundEvent =
            if (powered) SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON
            else SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF
}