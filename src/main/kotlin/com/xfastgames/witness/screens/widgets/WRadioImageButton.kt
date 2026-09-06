package com.xfastgames.witness.screens.widgets

import com.xfastgames.witness.Witness
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.resources.Identifier

class WRadioImageButton(
    var icon: Icon? = null,
    val group: WRadioGroup? = null,
    var isEnabled: Boolean = true,
    var isSelected: Boolean = false,
) : WWidget() {

    internal enum class RenderState { Normal, Selected, Disabled, Highlighted }

    private val texture = Identifier.fromNamespaceAndPath(Witness.IDENTIFIER, "textures/gui/toggle_image_button.png")
    private var isHovered = false

    init {
        setSize(16, 16)
        group?.add(this)
    }

    override fun canResize(): Boolean = false
    override fun canFocus(): Boolean = true

    override fun onClick(click: MouseButtonEvent, doubled: Boolean): InputResult {
        if (!isEnabled) return InputResult.IGNORED
        isSelected = !isSelected
        group?.select(this)
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK,
                1.0f
            )
        )
        return InputResult.PROCESSED
    }

    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        isHovered = mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height
        val renderState: RenderState = renderStateFor(isEnabled, isSelected, isHovered)

        val textureOffset: Float = renderState.ordinal * 0.25f
        val u1: Float = textureOffset
        val v1 = 0f
        val u2: Float = textureOffset + 0.25f
        val v2 = 1f

        ScreenDrawing.texturedRect(context, x, y, 16, 16, texture, u1, v1, u2, v2, -1)
        // A disabled tool is blank as well as dark: nothing it would draw can be placed, so its icon
        // has nothing to say (rules/minecraft/04-1-puzzle-composer-modifiers.md#the-empty-machine).
        if (isEnabled) icon?.paint(context, x, y, 16)
    }

    companion object {
        /**
         * Disabled wins over selected: a tool can stay armed in its group while the rail is dark,
         * so that it is the one lit when the rail comes back, without showing as lit meanwhile.
         */
        internal fun renderStateFor(enabled: Boolean, selected: Boolean, hovered: Boolean): RenderState = when {
            !enabled -> RenderState.Disabled
            selected -> RenderState.Selected
            hovered -> RenderState.Highlighted
            else -> RenderState.Normal
        }
    }
}
