package com.xfastgames.witness.screens.widgets

import com.xfastgames.witness.Witness
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

class WRadioImageButton(
    val icon: Icon? = null,
    val group: WRadioGroup? = null,
    var isEnabled: Boolean = true,
    var isSelected: Boolean = false,
) : WWidget() {

    private enum class RenderState { Normal, Selected, Disabled, Highlighted }

    private val texture = Identifier.of(Witness.IDENTIFIER, "textures/gui/toggle_image_button.png")
    private var isHovered = false

    init {
        setSize(16, 16)
        group?.add(this)
    }

    override fun canResize(): Boolean = false
    override fun canFocus(): Boolean = true

    override fun onClick(click: Click, doubled: Boolean): InputResult {
        if (!isEnabled) return InputResult.IGNORED
        isSelected = !isSelected
        group?.select(this)
        MinecraftClient.getInstance().soundManager.play(
            PositionedSoundInstance.ui(
                SoundEvents.UI_BUTTON_CLICK,
                1.0f
            )
        )
        return InputResult.PROCESSED
    }

    override fun paint(context: DrawContext, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        isHovered = mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height
        val renderState: RenderState = when {
            isSelected -> RenderState.Selected
            isHovered && isEnabled -> RenderState.Highlighted
            !isEnabled -> RenderState.Disabled
            else -> RenderState.Normal
        }

        val textureOffset: Float = renderState.ordinal * 0.25f
        val u1: Float = textureOffset
        val v1 = 0f
        val u2: Float = textureOffset + 0.25f
        val v2 = 1f

        ScreenDrawing.texturedRect(context, x, y, 16, 16, texture, u1, v1, u2, v2, -1)
        icon?.paint(context, x, y, 16)
    }
}
