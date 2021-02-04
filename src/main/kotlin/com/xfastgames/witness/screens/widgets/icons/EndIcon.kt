package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.circle
import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.util.math.MatrixStack

object EndIcon : Icon {
    override fun paint(matrices: MatrixStack, x: Int, y: Int, size: Int) {
        val bevel = 1
        val radius: Int = size / 5
        val thickness: Int = size / 3
        fill(
            matrices,
            x + bevel,
            y + thickness,
            x + bevel + thickness + bevel * 2,
            y + bevel + thickness * 2,
            .25f,
            .25f,
            .25f,
            1f
        )
        circle(matrices, x + size / 2, y + size / 2, radius, .25f, .25f, .25f, 1f, arc = 0..180)
    }
}