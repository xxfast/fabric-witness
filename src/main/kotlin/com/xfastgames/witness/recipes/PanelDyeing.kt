package com.xfastgames.witness.recipes

/**
 * Which of a panel's two colours a crafting grid is asking to change
 * (rules/minecraft/02-panel-dye.md#the-rule), from what is in it. Pure so the two recipes' match
 * rules can be pinned without booting Minecraft.
 */
object PanelDyeing {

    enum class Target { BACKGROUND, LINE }

    /**
     * One panel and one dye recolour the background; add exactly one glow ink sac and it is the
     * line instead. Anything else in the grid, or any count off by one, is no dye craft at all.
     */
    fun target(panels: Int, dyes: Int, glowInkSacs: Int, others: Int): Target? {
        if (others > 0 || panels != 1 || dyes != 1) return null
        return when (glowInkSacs) {
            0 -> Target.BACKGROUND
            1 -> Target.LINE
            else -> null
        }
    }
}
