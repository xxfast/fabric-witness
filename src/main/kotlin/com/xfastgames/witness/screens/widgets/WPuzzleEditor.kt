package com.xfastgames.witness.screens.widgets

import com.google.common.graph.EndpointPair
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.utils.circle
import com.xfastgames.witness.utils.fill
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.intersects
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private const val CLICK_PADDING = 0.2f

class WPuzzleEditor(
    private val inventory: Inventory,
    private val outputSlotIndex: Int
) : WWidget() {

    @Suppress("UnstableApiUsage")
    fun interface OnClickListener {
        fun onClick(node: Node?, edge: Edge?, edgeNodePair: EndpointPair<Node>?)
    }

    private val backgroundPainter: BackgroundPainter by lazy { BackgroundPainter.SLOT }

    private var onClickListener: OnClickListener? = null

    override fun getWidth(): Int = 18 * 6
    override fun getHeight(): Int = 18 * 6

    init {
        setSize(getWidth(), getHeight())
    }

    fun setClickListener(clickListener: OnClickListener) {
        onClickListener = clickListener
    }

    /**
     * TODO(migration): The pre-1.20 implementation rendered the live 3D puzzle panel into the GUI via
     * `client.bufferBuilders.entityVertexConsumers` + Tessellator. That immediate-mode path no longer
     * exists in the 1.21.6+ GUI pipeline, so the editor preview is now drawn with 2D DrawContext
     * primitives (same graph, flat styling). Verify in-game that the preview lines up with clicks.
     */
    @Environment(EnvType.CLIENT)
    @Suppress("UnstableApiUsage")
    override fun paint(context: DrawContext, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter.paintBackground(context, x, y, this)
        val puzzleStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (puzzleStack.isEmpty) return

        val puzzle: Panel = puzzleStack.panel ?: Panel.DEFAULT
        val scale: Int = max(puzzle.width, puzzle.height)

        // puzzle coordinates -> widget pixels (both axes mirrored, matching onClick's mapping)
        fun px(value: Float): Int = x + (width * (1 - value / scale)).roundToInt()
        fun py(value: Float): Int = y + (height * (1 - value / scale)).roundToInt()
        fun thickness(units: Float): Int = ((units / scale) * width).roundToInt().coerceAtLeast(1)

        val lineThickness: Int = thickness(4f / 16f)

        // Edges
        puzzle.graph.edges().forEach { side ->
            val edge: Edge = puzzle.graph.edgeValueOf(side) ?: return@forEach
            if (edge == Modifier.NONE || edge == Modifier.HIDDEN) return@forEach
            drawLine(context, px(side.nodeU().x), py(side.nodeU().y), px(side.nodeV().x), py(side.nodeV().y), lineThickness)
        }

        // Nodes
        puzzle.graph.nodes().forEach { node ->
            val radius: Int = if (node.modifier == Modifier.START) thickness(4f / 16f) else thickness(2f / 16f)
            circle(context, px(node.x), py(node.y), radius, .9f, .9f, .85f, 1f)
        }
    }

    private fun drawLine(context: DrawContext, x1: Int, y1: Int, x2: Int, y2: Int, thickness: Int) {
        val half: Int = thickness / 2
        when {
            x1 == x2 -> fill(
                context,
                x1 - half, minOf(y1, y2) - half, x1 + half, maxOf(y1, y2) + half,
                .9f, .9f, .85f, 1f
            )

            y1 == y2 -> fill(
                context,
                minOf(x1, x2) - half, y1 - half, maxOf(x1, x2) + half, y1 + half,
                .9f, .9f, .85f, 1f
            )

            else -> {
                // Diagonal edge: approximate with stepped segments
                val length: Int = hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).roundToInt()
                val steps: Int = (length / (half.coerceAtLeast(1))).coerceAtLeast(1)
                repeat(steps + 1) { step ->
                    val t: Float = step.toFloat() / steps
                    val cx: Int = (x1 + (x2 - x1) * t).roundToInt()
                    val cy: Int = (y1 + (y2 - y1) * t).roundToInt()
                    fill(context, cx - half, cy - half, cx + half, cy + half, .9f, .9f, .85f, 1f)
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    override fun onClick(click: Click, doubled: Boolean): InputResult {
        val x: Int = click.x().toInt()
        val y: Int = click.y().toInt()
        val inputStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (inputStack.isEmpty) return InputResult.IGNORED
        val inputPuzzle: Panel = inputStack.panel ?: return InputResult.IGNORED

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

        val edge: Edge? = edgeNodePair?.let { inputPuzzle.graph.edgeValueOf(it) }

        onClickListener?.onClick(node, edge, edgeNodePair)
        return InputResult.PROCESSED
    }
}
