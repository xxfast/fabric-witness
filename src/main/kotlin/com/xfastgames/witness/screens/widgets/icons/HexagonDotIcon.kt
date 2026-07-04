package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.hexagon
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.DrawContext

object HexagonDotIcon : Icon {
    override fun paint(context: DrawContext, x: Int, y: Int, size: Int) {
        // TODO: Fix this crap
        hexagon(context, x, y, 10, 1f, 1f, 1f, 1f)
    }
}
