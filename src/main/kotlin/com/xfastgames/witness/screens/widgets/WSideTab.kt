package com.xfastgames.witness.screens.widgets

import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents

/**
 * Which edge of the advancements tab strip a [WSideTab] borrows its sprite from. Vanilla only
 * ships the pieces a two-tab strip needs here: `tab_left_middle` (+ `_selected`) exists for strips
 * of three or more and is unused.
 */
enum class WSideTabPosition { TOP, BOTTOM }

private val TAB_LEFT_TOP: Identifier = Identifier.withDefaultNamespace("advancements/tab_left_top")
private val TAB_LEFT_TOP_SELECTED: Identifier = Identifier.withDefaultNamespace("advancements/tab_left_top_selected")
private val TAB_LEFT_BOTTOM: Identifier = Identifier.withDefaultNamespace("advancements/tab_left_bottom")
private val TAB_LEFT_BOTTOM_SELECTED: Identifier = Identifier.withDefaultNamespace("advancements/tab_left_bottom_selected")

private val PAINTER_TOP: BackgroundPainter by lazy { BackgroundPainter.createGuiSprite(TAB_LEFT_TOP) }
private val PAINTER_TOP_SELECTED: BackgroundPainter by lazy { BackgroundPainter.createGuiSprite(TAB_LEFT_TOP_SELECTED) }
private val PAINTER_BOTTOM: BackgroundPainter by lazy { BackgroundPainter.createGuiSprite(TAB_LEFT_BOTTOM) }
private val PAINTER_BOTTOM_SELECTED: BackgroundPainter by lazy { BackgroundPainter.createGuiSprite(TAB_LEFT_BOTTOM_SELECTED) }

private const val TAB_WIDTH = 32
private const val TAB_HEIGHT = 28
private const val ICON_OFFSET_X = 8
private const val ICON_OFFSET_Y = 6
private const val ICON_SIZE = 16

/**
 * A vanilla-style tab protruding from the outside left edge of a screen (advancements, the
 * creative inventory), rather than a tool button that lives inside the panel body
 * (rules/minecraft/04-puzzle-composer.md). Structurally mirrors [WRadioImageButton]: an icon, a
 * group, a selected flag, a click sound. It differs in two ways: it paints one of four 32x28
 * advancements sprites (picked by [position] and [isSelected]) instead of the toggle-button
 * texture, and a click always leaves it selected rather than toggling, since a tab strip with
 * nothing selected has no meaning.
 */
class WSideTab(
    val icon: Icon? = null,
    val position: WSideTabPosition,
    val group: WSideTabGroup? = null,
    var isSelected: Boolean = false,
) : WWidget() {

    init {
        setSize(TAB_WIDTH, TAB_HEIGHT)
        group?.add(this)
    }

    override fun canResize(): Boolean = false
    override fun canFocus(): Boolean = true

    override fun onClick(click: MouseButtonEvent, doubled: Boolean): InputResult {
        isSelected = true
        group?.select(this)
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        )
        return InputResult.PROCESSED
    }

    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val painter: BackgroundPainter = when (position) {
            WSideTabPosition.TOP -> if (isSelected) PAINTER_TOP_SELECTED else PAINTER_TOP
            WSideTabPosition.BOTTOM -> if (isSelected) PAINTER_BOTTOM_SELECTED else PAINTER_BOTTOM
        }
        painter.paintBackground(context, x, y, this)
        // The tab body is the left portion of the sprite; the rightmost few pixels are the seam
        // that meets the panel, so the icon sits left of centre rather than in the sprite's middle.
        icon?.paint(context, x + ICON_OFFSET_X, y + ICON_OFFSET_Y, ICON_SIZE)
    }
}

/**
 * Selection bookkeeping for a strip of [WSideTab]s, mirroring [WRadioGroup]. A separate class
 * rather than a shared generic one since the two widgets are otherwise unrelated.
 */
class WSideTabGroup {
    private val members: MutableList<WSideTab> = mutableListOf()

    /** Fired when the selection changes, with the tab that won. */
    var onSelected: ((WSideTab) -> Unit)? = null

    val selected: WSideTab? get() = members.firstOrNull { it.isSelected }

    fun add(member: WSideTab) {
        members.add(member)
    }

    fun select(selectedMember: WSideTab) {
        members.forEach { member -> if (member != selectedMember) member.isSelected = false }
        onSelected?.invoke(selectedMember)
    }
}
