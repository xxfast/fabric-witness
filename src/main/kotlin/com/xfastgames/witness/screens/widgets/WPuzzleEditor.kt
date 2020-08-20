package com.xfastgames.witness.screens.widgets

import com.xfastgames.witness.entities.PuzzleComposerBlockEntity
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.BlockInventory
import com.xfastgames.witness.utils.nextIn
import com.xfastgames.witness.utils.rotate
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import kotlin.math.truncate

class WPuzzleEditor(private val inventory: Inventory, private val slotIndex: Int) : WWidget() {

    override fun getWidth(): Int = 18 * 6
    override fun getHeight(): Int = 18 * 6

    private val client: MinecraftClient by lazy { MinecraftClient.getInstance() }
    private val backgroundPainter: BackgroundPainter by lazy { BackgroundPainter.SLOT }
    private val puzzlePanelRenderer: PuzzlePanelRenderer by lazy { PuzzlePanelRenderer }

    @Environment(EnvType.CLIENT)
    override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter.paintBackground(x, y, this)
        matrices.push()
        val puzzleStack: ItemStack = inventory.getStack(slotIndex)
        if (puzzleStack.isEmpty) return matrices.pop()

        /** TODO: Figure out why the puzzle background is not rendered in the render pass
         * Also, 🎩🐇 magic number land!
         */
        val immediateConsumer: VertexConsumerProvider.Immediate = client.bufferBuilders.entityVertexConsumers
        val puzzleScale = 6.75f
        matrices.scale(puzzleScale, -puzzleScale, puzzleScale)
        matrices.rotate(Vector3f.POSITIVE_Z, 180f)
        // Translate relative to panel placement
        matrices.translate(-1.25, -.75, .0)

        puzzlePanelRenderer.renderPanel(
            puzzleStack,
            matrices,
            immediateConsumer,
            15728880,
            OverlayTexture.DEFAULT_UV
        )

        matrices.translate(x.toDouble(), y.toDouble(), .0)
        matrices.scale(puzzleScale, -puzzleScale, puzzleScale)
        matrices.translate(.0, -1.0, .0)

        matrices.pop()
    }

    override fun onClick(x: Int, y: Int, button: Int) {
        val puzzleStack: ItemStack = inventory.getStack(slotIndex)
        if (puzzleStack.isEmpty) return
        val tag: CompoundTag = puzzleStack.tag ?: return
        val puzzle: Panel = tag.getPanel()
        val size: Int = puzzle.tiles.size
        val relativeX: Float = x / width.toFloat()
        val relativeY: Float = y / height.toFloat()
        val puzzleDX: Float = (relativeX / 6f) * size
        val puzzleDY: Float = (relativeY / 6f) * size
        val tileDX: Float = puzzleDX - truncate(puzzleDX)
        val tileDY: Float = puzzleDY - truncate(puzzleDY)
        val puzzleX: Int = (size - 1) - puzzleDX.toInt()
        val puzzleY: Int = (size - 1) - puzzleDY.toInt()
        val lineWidth: Float = 1 / 6f
        val lineRange: ClosedFloatingPointRange<Float> = (0.5f - lineWidth)..(0.5f + lineWidth)
        val isCenter: Boolean = tileDX in lineRange && tileDY in lineRange
        val isTop: Boolean = !isCenter && tileDY > 0.5f && tileDX in lineRange
        val isLeft: Boolean = !isCenter && tileDX > 0.5f && tileDY in lineRange
        val isRight: Boolean = !isCenter && tileDX < 0.5f && tileDY in lineRange
        val isBottom: Boolean = !isCenter && tileDY < 0.5f && tileDX in lineRange

        val isXBorder: Boolean = (puzzleX == 0 && isLeft) || (puzzleX == size - 1 && isRight)
        val isYBorder: Boolean = (puzzleY == 0 && isTop) || (puzzleY == size - 1 && isBottom)

        // Tile update logic
        var updatedPuzzle: Panel = puzzle.put(puzzleX, puzzleY) {

            val start: Boolean = if (isCenter) !isStart else isStart

            // End is only allowed on the edges of the puzzle
            val top: Line? = when {
                isYBorder && isTop -> top.nextIn(Line.END, null)
                isTop -> top.nextIn(Line.FILLED, Line.SHORTENED, null)
                else -> top
            }

            val left: Line? = when {
                isXBorder && isLeft -> left.nextIn(Line.END, null)
                isLeft -> left.nextIn(Line.FILLED, Line.SHORTENED, null)
                else -> left
            }

            val bottom: Line? = when {
                isYBorder && isBottom -> bottom.nextIn(Line.END, null)
                isBottom -> bottom.nextIn(Line.FILLED, Line.SHORTENED, null)
                else -> bottom
            }

            val right: Line? = when {
                isXBorder && isRight -> right.nextIn(Line.END, null)
                isRight -> right.nextIn(Line.FILLED, Line.SHORTENED, null)
                else -> right
            }

            return@put Tile(start, top, left, bottom, right)
        }

        // Update neighbours
        updatedPuzzle = when {
            isTop -> updatedPuzzle.put(puzzleX, puzzleY - 1) {
                this.apply { bottom = updatedPuzzle.tiles[puzzleX][puzzleY].top }
            }

            isLeft -> updatedPuzzle.put(puzzleX - 1, puzzleY) {
                this.apply { right = updatedPuzzle.tiles[puzzleX][puzzleY].left }
            }

            isBottom -> updatedPuzzle.put(puzzleX, puzzleY + 1) {
                this.apply { top = updatedPuzzle.tiles[puzzleX][puzzleY].bottom }
            }

            isRight -> updatedPuzzle.put(puzzleX + 1, puzzleY) {
                this.apply { left = updatedPuzzle.tiles[puzzleX][puzzleY].right }
            }

            else -> updatedPuzzle
        }

        tag.putPanel(updatedPuzzle)
        inventory.setStack(slotIndex, puzzleStack)
        require(inventory is BlockInventory)
        require(inventory.owner is PuzzleComposerBlockEntity)
        inventory.owner.syncInventorySlotTag(slotIndex, tag)
    }
}