package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Placeholder tab icon for the Grid tab (rules/minecraft/04-2-puzzle-composer-grid.md): a 2x2 lattice. */
object GridTabIcon : Icon {
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int) {
        val dot: Int = (size / 6).coerceAtLeast(1)
        val positions: List<Int> = listOf(size / 4, size * 3 / 4)
        positions.forEach { dx ->
            positions.forEach { dy ->
                fill(
                    context,
                    x + dx - dot / 2,
                    y + dy - dot / 2,
                    x + dx + dot / 2 + 1,
                    y + dy + dot / 2 + 1,
                    .25f,
                    .25f,
                    .25f,
                    1f
                )
            }
        }
    }
}
