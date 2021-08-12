package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.hexagon
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.util.math.MatrixStack

object HexagonDotIcon : Icon {
    override fun paint(matrices: MatrixStack, x: Int, y: Int, size: Int) {
        val bevel = 1
        val thickness: Int = size / 3
        // TODO: Fix this crap
//        fill(matrices, x + bevel, y + thickness, x - bevel + size, y + bevel + thickness * 2, .25f, .25f, .25f, 1f)
        hexagon(matrices, x, y, 10, 1f, 1f, 1f, 1f)
    }
}