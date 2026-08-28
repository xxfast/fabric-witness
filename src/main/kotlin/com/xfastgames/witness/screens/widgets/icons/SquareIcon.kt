package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.GuiGraphicsExtractor

/** The square tool (rules/witness/06-colored-squares.md): a filled square, sized like StartIcon's disc. */
object SquareIcon : Icon {
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int) {
        val side: Int = size * 2 / 3
        val left: Int = x + (size - side) / 2
        val top: Int = y + (size - side) / 2
        fill(context, left, top, left + side, top + side, ICON_BODY, ICON_BODY, ICON_BODY, 1f)
    }
}
