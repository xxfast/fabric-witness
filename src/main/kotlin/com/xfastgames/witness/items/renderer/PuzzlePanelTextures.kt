package com.xfastgames.witness.items.renderer

import com.xfastgames.witness.Witness
import net.minecraft.world.item.DyeColor
import net.minecraft.resources.Identifier
import java.util.Locale

/**
 * Textures shared by the world-panel renderer and the composer's 2D panel preview.
 */
object PuzzlePanelTextures {
    val lineFill: Identifier =
        Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "textures/entity/puzzle_panel_line_fill.png")
    val solutionFill: Identifier =
        Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "textures/entity/puzzle_panel_solution_fill.png")

    fun backdrop(color: DyeColor): Identifier =
        Identifier.fromNamespaceAndPath(
            Witness.IDENTIFIER,
            "textures/entity/puzzle_panel_backdrop_${color.name.lowercase(Locale.ROOT)}.png"
        )
}
