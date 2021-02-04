package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.util.math.MatrixStack

object BreakIcon : Icon {
    override fun paint(matrices: MatrixStack, x: Int, y: Int, size: Int) {
        val bevel = 1
        val thickness: Int = size / 3
        fill(matrices, x + bevel, y + thickness, x + bevel + thickness, y + bevel + thickness * 2, .25f, .25f, .25f, 1f)
        fill(
            matrices,
            x + thickness * 2,
            y + thickness,
            x + thickness * 3,
            y + bevel + thickness * 2,
            .25f,
            .25f,
            .25f,
            1f
        )
    }
}