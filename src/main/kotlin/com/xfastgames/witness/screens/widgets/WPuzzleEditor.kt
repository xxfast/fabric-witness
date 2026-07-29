package com.xfastgames.witness.screens.widgets

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.Symbol
import com.xfastgames.witness.items.data.panel
import com.xfastgames.witness.items.renderer.PuzzlePanelTextures
import com.xfastgames.witness.utils.fill
import com.xfastgames.witness.utils.hexagon
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.incidentEdges
import com.xfastgames.witness.utils.intersects
import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.InputResult
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private const val CLICK_PADDING = 0.2f
private const val GRAPH_RED = .25f
private const val GRAPH_GREEN = .25f
private const val GRAPH_BLUE = .25f
private const val SOLUTION_RED = .95f
private const val SOLUTION_GREEN = .95f
private const val SOLUTION_BLUE = .9f
/**
 * A hexagon is drawn narrower than the line it marks so the line still shows either side of it.
 * Since it draws in the panel's background colour, at full line width it would sever the line and
 * read as a broken edge (rules/witness/03-broken-edges.md) rather than a symbol on an intact one.
 */
private const val HEXAGON_LINE_FRACTION = 3f / 4f
private const val TEXTURE_COLOR = -1

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
     * The editor stays in DrawContext space so its pixels and click targets share one transform.
     * Its backdrop, graph shapes, edge modifiers, and solution overlay deliberately mirror
     * [com.xfastgames.witness.items.renderer.PuzzlePanelRenderer].
     */
    @Environment(EnvType.CLIENT)
    @Suppress("UnstableApiUsage")
    override fun paint(context: DrawContext, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter.paintBackground(context, x, y, this)
        val puzzleStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (puzzleStack.isEmpty) return

        val puzzle: Panel = puzzleStack.panel ?: Panel.DEFAULT
        val scale: Int = max(puzzle.width, puzzle.height)

        ScreenDrawing.texturedRect(
            context,
            x,
            y,
            width,
            height,
            PuzzlePanelTextures.backdrop(puzzle.backgroundColor),
            TEXTURE_COLOR
        )

        // puzzle coordinates -> widget pixels (both axes mirrored, matching onClick's mapping)
        fun px(value: Float): Int = x + (width * (1 - value / scale)).roundToInt()
        fun py(value: Float): Int = y + (height * (1 - value / scale)).roundToInt()
        fun thickness(units: Float): Int = ((units / scale) * width).roundToInt().coerceAtLeast(1)

        val lineThickness: Int = thickness(4f / 16f)

        drawGraph(context, puzzle.graph, ::px, ::py, lineThickness)
        drawSolution(context, puzzle.line, ::px, ::py, lineThickness)
        // Symbols draw last, over the solution as well as the grid: a hexagon stays visible once
        // the line covers it, which is the only way a player can tell it was crossed
        // (rules/witness/04-hexagon-dots.md).
        drawSymbols(context, puzzle, ::px, ::py, lineThickness)
    }

    private fun drawGraph(
        context: DrawContext,
        graph: ValueGraph<Node, Edge>,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        graph.nodes().forEach { node ->
            val visibleEdges: Int = graph.incidentEdges(node).count { side ->
                graph.edgeValueOf(side)?.modifier !in listOf(Modifier.NONE, Modifier.HIDDEN)
            }
            when {
                node.modifier == Modifier.START ->
                    drawCircle(context, px(node.x), py(node.y), lineThickness * 2, solution = false)

                // Rounds off the tip of an end point's nub; its edge draws like any other.
                node.modifier == Modifier.END ->
                    drawCircle(context, px(node.x), py(node.y), lineThickness, solution = false)

                visibleEdges > 1 ->
                    drawCircle(context, px(node.x), py(node.y), lineThickness, solution = false)

                visibleEdges == 1 ->
                    drawSquare(context, px(node.x), py(node.y), lineThickness, PuzzlePanelTextures.lineFill)
            }
        }

        graph.edges().forEach { side ->
            when ((graph.edgeValueOf(side) ?: return@forEach).modifier) {
                // A start point is a node role, never an edge value. A legacy panel that stored one
                // on an edge draws as a plain segment (rules/witness/01-start-points.md).
                Modifier.NORMAL, Modifier.START ->
                    drawLine(context, side, px, py, lineThickness, PuzzlePanelTextures.lineFill)

                Modifier.BREAK -> drawBrokenLine(context, side, px, py, lineThickness)

                Modifier.NONE, Modifier.DOT, Modifier.END, Modifier.HIDDEN -> Unit
            }
        }
    }

    private fun drawSymbols(
        context: DrawContext,
        puzzle: Panel,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        val graph: ValueGraph<Node, Edge> = puzzle.graph
        // The panel's own backdrop colour, so a hexagon reads as a notch punched through whatever
        // covers it: the grid line while untraced, the solution line once drawn.
        val color: Int = 0xFF000000.toInt() or (puzzle.backgroundColor.entityColor and 0xFFFFFF)
        val diameter: Int = (lineThickness * HEXAGON_LINE_FRACTION).roundToInt().coerceAtLeast(1)

        graph.nodes()
            .filter { node -> node.symbol == Symbol.HEXAGON }
            .forEach { node -> hexagon(context, px(node.x), py(node.y), diameter, color) }

        graph.edges().forEach { side ->
            val edge: Edge = graph.edgeValueOf(side) ?: return@forEach
            if (edge.symbol != Symbol.HEXAGON) return@forEach
            val u: Node = side.nodeU()
            val v: Node = side.nodeV()
            // An edge hexagon marks the edge as a whole, so it sits at the midpoint.
            hexagon(context, px((u.x + v.x) / 2), py((u.y + v.y) / 2), diameter, color)
        }
    }

    private fun drawSolution(
        context: DrawContext,
        line: Graph<Node>,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        line.nodes().forEach { node ->
            val diameter: Int = if (node.modifier == Modifier.START) lineThickness * 2 else lineThickness
            drawCircle(context, px(node.x), py(node.y), diameter, solution = true)
        }
        line.edges().forEach { side ->
            drawLine(context, side, px, py, lineThickness, PuzzlePanelTextures.solutionFill)
        }
    }

    private fun drawBrokenLine(
        context: DrawContext,
        side: EndpointPair<Node>,
        px: (Float) -> Int,
        py: (Float) -> Int,
        thickness: Int
    ) {
        val x1: Float = px(side.nodeU().x).toFloat()
        val y1: Float = py(side.nodeU().y).toFloat()
        val x2: Float = px(side.nodeV().x).toFloat()
        val y2: Float = py(side.nodeV().y).toFloat()
        val length: Float = hypot(x2 - x1, y2 - y1)
        if (length <= thickness) return

        val gapFraction: Float = thickness / length
        val firstEnd = .5f - gapFraction / 2
        val secondStart = .5f + gapFraction / 2
        drawLine(context, x1, y1, lerp(x1, x2, firstEnd), lerp(y1, y2, firstEnd), thickness, PuzzlePanelTextures.lineFill)
        drawLine(context, lerp(x1, x2, secondStart), lerp(y1, y2, secondStart), x2, y2, thickness, PuzzlePanelTextures.lineFill)
    }

    private fun drawLine(
        context: DrawContext,
        side: EndpointPair<Node>,
        px: (Float) -> Int,
        py: (Float) -> Int,
        thickness: Int,
        texture: Identifier
    ) {
        drawLine(
            context,
            px(side.nodeU().x).toFloat(),
            py(side.nodeU().y).toFloat(),
            px(side.nodeV().x).toFloat(),
            py(side.nodeV().y).toFloat(),
            thickness,
            texture
        )
    }

    private fun drawLine(
        context: DrawContext,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        thickness: Int,
        texture: Identifier
    ) {
        val dx: Float = x2 - x1
        val dy: Float = y2 - y1
        val length: Int = hypot(dx, dy).roundToInt().coerceAtLeast(1)
        if (abs(dy) < .001f) {
            ScreenDrawing.texturedRect(
                context,
                minOf(x1, x2).roundToInt(),
                y1.roundToInt() - thickness / 2,
                length,
                thickness,
                texture,
                TEXTURE_COLOR
            )
            return
        }
        if (abs(dx) < .001f) {
            ScreenDrawing.texturedRect(
                context,
                x1.roundToInt() - thickness / 2,
                minOf(y1, y2).roundToInt(),
                thickness,
                length,
                texture,
                TEXTURE_COLOR
            )
            return
        }

        val matrices = context.matrices
        matrices.pushMatrix()
        matrices.translate(x1, y1)
        matrices.rotate(atan2(dy, dx))
        ScreenDrawing.texturedRect(
            context,
            0,
            -thickness / 2,
            length,
            thickness,
            texture,
            TEXTURE_COLOR
        )
        matrices.popMatrix()
    }

    private fun drawSquare(
        context: DrawContext,
        centerX: Int,
        centerY: Int,
        size: Int,
        texture: Identifier
    ) {
        ScreenDrawing.texturedRect(
            context,
            centerX - size / 2,
            centerY - size / 2,
            size,
            size,
            texture,
            TEXTURE_COLOR
        )
    }

    /**
     * Rasterizes a disc into an exact [diameter]-pixel box. This matters for odd line widths:
     * rounding a 4.5px radius to 5 made every 9px junction protrude by one pixel.
     */
    private fun drawCircle(context: DrawContext, x: Int, y: Int, diameter: Int, solution: Boolean) {
        val red: Float = if (solution) SOLUTION_RED else GRAPH_RED
        val green: Float = if (solution) SOLUTION_GREEN else GRAPH_GREEN
        val blue: Float = if (solution) SOLUTION_BLUE else GRAPH_BLUE
        val radius: Float = diameter / 2f
        val left: Int = x - diameter / 2
        val top: Int = y - diameter / 2

        repeat(diameter) { row ->
            val dy: Float = row + .5f - radius
            var firstPixel = -1
            var lastPixel = -1
            repeat(diameter) { column ->
                val dx: Float = column + .5f - radius
                if (dx * dx + dy * dy <= radius * radius) {
                    if (firstPixel == -1) firstPixel = column
                    lastPixel = column
                }
            }
            if (firstPixel != -1) {
                fill(
                    context,
                    left + firstPixel,
                    top + row,
                    left + lastPixel + 1,
                    top + row + 1,
                    red,
                    green,
                    blue,
                    1f
                )
            }
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

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
