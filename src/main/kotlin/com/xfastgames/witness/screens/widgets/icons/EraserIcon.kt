package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * The Grid tab's eraser, drawn as a close-up of the **other end of [PencilIcon]'s pencil**: the
 * rubber nub, the ferrule crimped around it, then the barrel running off the bottom of the crop.
 *
 * Same pencil, same barrel width, opposite end, so the two icons read as one object rather than a
 * pencil next to a loose block of rubber. The silhouettes stay unmistakable at 16px because one
 * narrows to a point and the other is blunt and widest at the top.
 */
object EraserIcon : Icon {
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int) {
        val unit: Int = maxOf(1, size / 16)

        fun block(left: Int, top: Int, right: Int, bottom: Int, shade: Float) = fill(
            context,
            x + left * unit,
            y + top * unit,
            x + right * unit,
            y + bottom * unit,
            shade, shade, shade, 1f
        )

        block(6, 2, 10, 3, ICON_BODY)     // rounded off, a unit in on each side
        block(5, 3, 11, 7, ICON_BODY)     // the rubber, sitting proud of the barrel
        block(5, 7, 11, 9, ICON_ACCENT)   // ferrule, the one detail the silhouette cannot carry
        block(6, 9, 10, 14, ICON_BODY)    // barrel, cut off by the bottom of the crop
    }
}
