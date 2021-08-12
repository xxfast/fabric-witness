package com.xfastgames.witness.screens.widgets

import com.google.common.graph.EndpointPair
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.getPanel
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.intersects
import com.xfastgames.witness.utils.rotate
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3f

private const val CLICK_PADDING = 0.2f

class WPuzzleEditor(
    private val inventory: Inventory,
    private val outputSlotIndex: Int
) : WWidget() {

    @Suppress("UnstableApiUsage")
    fun interface OnClickListener {
        fun onClick(node: Node?, edge: Edge?, edgeNodePair: EndpointPair<Node>?)
    }

    private val client: MinecraftClient by lazy { MinecraftClient.getInstance() }
    private val backgroundPainter: BackgroundPainter by lazy { BackgroundPainter.SLOT }
    private val puzzlePanelRenderer: PuzzlePanelRenderer by lazy { PuzzlePanelRenderer }

    private var onClickListener: OnClickListener? = null

    override fun getWidth(): Int = 18 * 6
    override fun getHeight(): Int = 18 * 6

    init {
        setSize(getWidth(), getHeight())
    }

    fun setClickListener(clickListener: OnClickListener) {
        onClickListener = clickListener
    }

    @Environment(EnvType.CLIENT)
    override fun paint(matrices: MatrixStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter.paintBackground(matrices, x, y, this)
        matrices.push()
        val puzzleStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (puzzleStack.isEmpty) return matrices.pop()

        /** TODO: Figure out why the puzzle background is not rendered in the render pass
         * Also, 🎩🐇 magic number land!
         */
        val immediateConsumer: VertexConsumerProvider.Immediate = client.bufferBuilders.entityVertexConsumers
        val puzzleScale = 6.75f
        matrices.scale(puzzleScale, -puzzleScale, puzzleScale)
        matrices.rotate(Vec3f.POSITIVE_Z, 180f)
        // Translate relative to panel placement
        matrices.translate(-1.275, -.895, .0)

        val puzzle: Panel = puzzleStack.nbt?.getPanel(KEY_PANEL) ?: Panel.DEFAULT

        puzzlePanelRenderer.renderGraph(
            graph = puzzle.graph,
            width = puzzle.width,
            height = puzzle.height,
            matrices = matrices,
            vertexConsumers = immediateConsumer,
            light = 15728880,
            overlay = OverlayTexture.DEFAULT_UV
        )

        val tessellator: Tessellator? = Tessellator.getInstance()
        val bufferBuilder = tessellator!!.buffer
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.color(0, 0, 0, 1).next()
        puzzlePanelRenderer.renderHighlighted(
            graph = puzzle.graph,
            width = puzzle.width,
            height = puzzle.height,
            matrices = matrices,
            vertexConsumer = bufferBuilder,
            light = 15728880,
            overlay = OverlayTexture.DEFAULT_UV
        )
        tessellator.draw()

        matrices.translate(x.toDouble(), y.toDouble(), .0)
        matrices.scale(puzzleScale, -puzzleScale, puzzleScale)
        matrices.translate(.0, -1.0, .0)

        matrices.pop()
    }

    @Suppress("UnstableApiUsage")
    override fun onClick(x: Int, y: Int, button: Int): InputResult {
        val inputStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (inputStack.isEmpty) return InputResult.IGNORED
        val inputPuzzle: Panel = inputStack.nbt?.getPanel(KEY_PANEL) ?: return InputResult.IGNORED

        val xPosition = 1 - (x.toFloat() / width)
        val yPosition = 1 - (y.toFloat() / height)

        val scale: Int = maxOf(inputPuzzle.width, inputPuzzle.height)
        val puzzleRelativeX: Float = xPosition * scale
        val puzzleRelativeY: Float = yPosition * scale

        val mouseXRange: ClosedFloatingPointRange<Float> =
            (puzzleRelativeX - CLICK_PADDING)..(puzzleRelativeX + CLICK_PADDING)

        val mouseYRange: ClosedFloatingPointRange<Float> =
            (puzzleRelativeY - CLICK_PADDING)..(puzzleRelativeY + CLICK_PADDING)

        val node: Node? = inputPuzzle.graph.nodes().find { node ->
            node.x in mouseXRange && node.y in mouseYRange
        }

        val edgeNodePair: EndpointPair<Node>? = inputPuzzle.graph.edges()
            .find { nodePair ->
                val u: Node = nodePair.nodeU()
                val v: Node = nodePair.nodeV()
                val edgeXRange: ClosedFloatingPointRange<Float> = u.x..v.x
                val edgeYRange: ClosedFloatingPointRange<Float> = u.y..v.y
                val xIntersects: Boolean = mouseXRange intersects edgeXRange
                val yIntersects: Boolean = mouseYRange intersects edgeYRange
                val result: Boolean = xIntersects && yIntersects
                result
            }

        val edge: Edge? = edgeNodePair?.let { inputPuzzle.graph.edgeValue(it).orElse(null) }

        onClickListener?.onClick(node, edge, edgeNodePair)
        return InputResult.PROCESSED
    }
}