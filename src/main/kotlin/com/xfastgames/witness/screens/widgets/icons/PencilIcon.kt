package com.xfastgames.witness.screens.widgets.icons

import com.xfastgames.witness.utils.fill
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * The Grid tab's pencil, drawn as a close-up of the **sharpened tip**: a barrel cropped by the top
 * edge, the ring where the sharpening starts, then the cone tapering to a point.
 *
 * A whole pencil shrunk into 16px is a diagonal smudge, so the icon zooms until the business end
 * fills the box. [EraserIcon] is the same pencil seen from the other end, which is what makes the
 * pair read as one object rather than two unrelated tools.
 *
 * Laid out on a 16-unit grid scaled by `size`, since that is what an [Icon] is handed.
 */
object PencilIcon : Icon {
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

        block(5, 2, 11, 5, ICON_BODY)     // barrel, cut off by the top of the crop
        block(5, 5, 11, 6, ICON_ACCENT)   // the ring where the wood was cut back
        block(5, 6, 11, 8, ICON_BODY)     // cone, stepped in a unit a side at a time
        block(6, 8, 10, 10, ICON_BODY)
        block(7, 10, 9, 14, ICON_BODY)    // the point itself
    }
}
