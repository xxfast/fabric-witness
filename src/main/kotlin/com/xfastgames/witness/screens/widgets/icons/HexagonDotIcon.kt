package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.hexagon
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.DrawContext

object HexagonDotIcon : Icon {
    override fun paint(context: DrawContext, x: Int, y: Int, size: Int) {
        // Sized and coloured like StartIcon's disc, so the toolbar reads as one set.
        hexagon(context, x + size / 2, y + size / 2, size * 2 / 3, .25f, .25f, .25f, 1f)
    }
}
