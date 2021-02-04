package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.circle
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.util.math.MatrixStack

object StartIcon : Icon {
    override fun paint(matrices: MatrixStack, x: Int, y: Int, size: Int) {
        val radius: Int = size / 3
        circle(matrices, x + size / 2, y + size / 2, radius, .25f, .25f, .25f, 1f)
    }
}