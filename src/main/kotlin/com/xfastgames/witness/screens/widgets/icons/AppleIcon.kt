package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.circle
import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * The hexagon tool's face on a tree panel (rules/minecraft/04-1-puzzle-composer-modifiers.md,
 * "what each type's rail holds"): the same mark, drawn as the apple it is there. Fruit sized like
 * [StartIcon]'s disc so the rail reads as one set; the fruit's red sits at body luminance on the
 * button, the leaf is the one accent.
 */
object AppleIcon : Icon {
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int) {
        val centerX: Int = x + size / 2
        val centerY: Int = y + size / 2 + 1
        val radius: Int = size / 3
        // Stem straight up out of the fruit, then a leaf off to its right.
        fill(context, centerX - 1, centerY - radius - 3, centerX + 1, centerY - radius + 1, ICON_BODY, ICON_BODY, ICON_BODY, 1f)
        circle(context, centerX + 3, centerY - radius - 2, 2, ICON_ACCENT, ICON_ACCENT, ICON_ACCENT, 1f)
        circle(context, centerX, centerY, radius, .75f, .12f, .10f, 1f)
    }
}
