package com.xfastgames.witness.screens.widgets

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.items.data.Edge
import com.xfastgames.witness.items.data.Modifier
import com.xfastgames.witness.items.data.Node
import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.items.data.Atom
import com.xfastgames.witness.items.data.anchorPathBetween
import com.xfastgames.witness.items.data.anchors
import com.xfastgames.witness.items.data.nearestJoinablePair
import com.xfastgames.witness.items.data.nodeAt
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
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.Identifier
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private const val CLICK_PADDING = 0.2f

/**
 * How near a Grid-tab drag has to pass an anchor to paint it, in panel units. Half a cell, so every
 * point on the panel snaps to some anchor once a stroke is under way: a pencil that only registered
 * within [CLICK_PADDING] of a centre would drop steps the moment the cursor drifted off the line.
 * Clicks stay on the tighter [CLICK_PADDING], where landing between anchors should do nothing.
 */
private const val DRAG_PADDING = 0.5f
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
/**
 * Anchor dots (rules/minecraft/04-2-puzzle-composer-grid.md) mark where a node *may* sit, not where
 * one does, so they draw at a fraction of a real node's opacity: present enough to aim at, faint
 * enough never to be mistaken for the node itself.
 */
private const val ANCHOR_DOT_ALPHA = 0.35f

class WPuzzleEditor(
    private val inventory: Container,
    private val outputSlotIndex: Int
) : WWidget() {

    /** Which tab this editor is currently serving: paint meaning onto the graph, or shape it. */
    enum class EditorMode { MODIFIERS, GRID }

    @Suppress("UnstableApiUsage")
    fun interface OnClickListener {
        fun onClick(node: Node?, edge: Edge?, edgeNodePair: EndpointPair<Node>?)
    }

    /** Reports an anchor *position*, in panel coordinates, clicked on the Grid tab. */
    fun interface OnAnchorClickListener {
        fun onAnchorClick(x: Float, y: Float)
    }

    /**
     * Reports a Grid-tab gesture aimed at the segment between two adjacent anchor *positions*.
     *
     * Fires for a click that landed on a segment rather than a node, and once per anchor crossed
     * during a drag, so a sweep paints along its whole stroke instead of only between where the
     * press and the release landed. Both mean the same thing to the panel, so they share a listener.
     */
    fun interface OnSegmentListener {
        fun onSegment(fromX: Float, fromY: Float, toX: Float, toY: Float)
    }

    private val backgroundPainter: BackgroundPainter by lazy { BackgroundPainter.SLOT }

    private var onClickListener: OnClickListener? = null
    private var onAnchorClickListener: OnAnchorClickListener? = null
    private var onSegmentListener: OnSegmentListener? = null

    var mode: EditorMode = EditorMode.MODIFIERS

    /** What a Grid-tab press landed on: an anchor, or failing that the segment nearest the cursor. */
    private var pressedAnchor: Node? = null
    private var pressedSegment: Pair<Node, Node>? = null

    /** The last anchor a drag painted, and whether it painted anything at all. */
    private var lastPaintedAnchor: Node? = null
    private var paintedDuringDrag: Boolean = false

    override fun getWidth(): Int = 18 * 6
    override fun getHeight(): Int = 18 * 6

    init {
        setSize(getWidth(), getHeight())
    }

    fun setClickListener(clickListener: OnClickListener) {
        onClickListener = clickListener
    }

    fun setAnchorClickListener(anchorClickListener: OnAnchorClickListener) {
        onAnchorClickListener = anchorClickListener
    }

    fun setSegmentListener(segmentListener: OnSegmentListener) {
        onSegmentListener = segmentListener
    }

    /**
     * The editor stays in GuiGraphicsExtractor space so its pixels and click targets share one transform.
     * Its backdrop, graph shapes, edge modifiers, and solution overlay deliberately mirror
     * [com.xfastgames.witness.items.renderer.PuzzlePanelRenderer].
     */
    @Environment(EnvType.CLIENT)
    @Suppress("UnstableApiUsage")
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        backgroundPainter.paintBackground(context, x, y, this)
        val puzzleStack: ItemStack = inventory.getItem(outputSlotIndex)
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

        // A finished panel can never show a node that isn't there. This tab is the one place that
        // does, so it draws under everything else the Modifiers tab already shows.
        if (mode == EditorMode.GRID) drawLattice(context, puzzle, ::px, ::py, lineThickness)

        drawGraph(context, puzzle.graph, ::px, ::py, lineThickness)
        drawSolution(context, puzzle.line, ::px, ::py, lineThickness)
        // Symbols draw last, over the solution as well as the grid: a hexagon stays visible once
        // the line covers it, which is the only way a player can tell it was crossed
        // (rules/witness/04-hexagon-dots.md).
        drawSymbols(context, puzzle, ::px, ::py, lineThickness)
    }

    /**
     * The lattice under the panel, and the nodes sitting on it that nothing else draws. Grid tab
     * only: the item, the block face, and the panel in the world never show either.
     *
     * A faint dot goes at **every** anchor, occupied or not, so it means "a node can go here"
     * rather than "no node is here". That distinction is the whole point: a mark that appears only
     * where something is *missing* inverts the panel's own language, where a dot has always meant a
     * node. Drawn first, so anything real paints over it.
     *
     * On top of that, a solid dot at every node with nothing joined to it. [drawGraph] draws such a
     * node as nothing at all, correctly, since it is invisible on a finished panel, which would
     * otherwise leave it indistinguishable from a bare anchor while answering the tools completely
     * differently.
     */
    private fun drawLattice(
        context: GuiGraphicsExtractor,
        puzzle: Panel,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        val anchorDiameter: Int = (lineThickness / 2f).roundToInt().coerceAtLeast(1)
        puzzle.anchors().forEach { anchor ->
            drawCircle(
                context,
                px(anchor.x),
                py(anchor.y),
                anchorDiameter,
                solution = false,
                alpha = ANCHOR_DOT_ALPHA
            )
        }

        // At a junction's own size rather than the anchor dot's, because it is a real node.
        // START and END carry their own shapes in drawGraph even with nothing joined to them.
        puzzle.graph.nodes()
            .filter { node ->
                node.modifier != Modifier.START &&
                    node.modifier != Modifier.END &&
                    puzzle.graph.visibleEdgeCount(node) == 0
            }
            .forEach { node ->
                drawCircle(context, px(node.x), py(node.y), lineThickness, solution = false)
            }
    }

    private fun drawGraph(
        context: GuiGraphicsExtractor,
        graph: ValueGraph<Node, Edge>,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        graph.nodes().forEach { node ->
            val visibleEdges: Int = graph.visibleEdgeCount(node)
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
        context: GuiGraphicsExtractor,
        puzzle: Panel,
        px: (Float) -> Int,
        py: (Float) -> Int,
        lineThickness: Int
    ) {
        val graph: ValueGraph<Node, Edge> = puzzle.graph
        // The panel's own backdrop colour, so a hexagon reads as a notch punched through whatever
        // covers it: the grid line while untraced, the solution line once drawn.
        val color: Int = 0xFF000000.toInt() or (puzzle.backgroundColor.getTextureDiffuseColor() and 0xFFFFFF)
        val diameter: Int = (lineThickness * HEXAGON_LINE_FRACTION).roundToInt().coerceAtLeast(1)

        graph.nodes()
            .filter { node -> node.symbol == Atom.HEXAGON }
            .forEach { node -> hexagon(context, px(node.x), py(node.y), diameter, color) }

        graph.edges().forEach { side ->
            val edge: Edge = graph.edgeValueOf(side) ?: return@forEach
            if (edge.symbol != Atom.HEXAGON) return@forEach
            val u: Node = side.nodeU()
            val v: Node = side.nodeV()
            // An edge hexagon marks the edge as a whole, so it sits at the midpoint.
            hexagon(context, px((u.x + v.x) / 2), py((u.y + v.y) / 2), diameter, color)
        }
    }

    private fun drawSolution(
        context: GuiGraphicsExtractor,
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
        context: GuiGraphicsExtractor,
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
        context: GuiGraphicsExtractor,
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
        context: GuiGraphicsExtractor,
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

        val matrices = context.pose()
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
        context: GuiGraphicsExtractor,
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
    private fun drawCircle(
        context: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        diameter: Int,
        solution: Boolean,
        alpha: Float = 1f
    ) {
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
                    alpha
                )
            }
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    /**
     * How many of [node]'s segments actually draw. A `NONE` or `HIDDEN` edge is still in the graph
     * but invisible, so a node holding only those reads as joined to nothing, exactly as if the
     * edges had been erased.
     */
    @Suppress("UnstableApiUsage")
    private fun ValueGraph<Node, Edge>.visibleEdgeCount(node: Node): Int =
        incidentEdges(node).count { side ->
            edgeValueOf(side)?.modifier !in listOf(Modifier.NONE, Modifier.HIDDEN)
        }

    @Suppress("UnstableApiUsage")
    override fun onMouseDown(click: MouseButtonEvent, doubled: Boolean): InputResult {
        if (mode != EditorMode.GRID) return InputResult.IGNORED

        val puzzle: Panel = outputPanel() ?: return InputResult.IGNORED
        val (x: Float, y: Float) = panelPosition(click, puzzle)

        // A node wins over a segment, since a position near a node is near every segment meeting
        // it. Only when the press missed every anchor does it fall through to a segment.
        pressedAnchor = nearestAnchor(click, puzzle, CLICK_PADDING)
        pressedSegment =
            if (pressedAnchor != null) null
            else puzzle.nearestJoinablePair(x, y, CLICK_PADDING)

        // A drag may start off-anchor and still be a stroke, so the walk starts from whatever
        // anchor is nearest under the press, at the looser drag tolerance.
        lastPaintedAnchor = nearestAnchor(click, puzzle, DRAG_PADDING)
        paintedDuringDrag = false
        return InputResult.PROCESSED
    }

    @Suppress("UnstableApiUsage")
    override fun onMouseDrag(click: MouseButtonEvent, offsetX: Double, offsetY: Double): InputResult {
        if (mode != EditorMode.GRID) return InputResult.IGNORED

        val puzzle: Panel = outputPanel() ?: return InputResult.IGNORED
        val current: Node = nearestAnchor(click, puzzle, DRAG_PADDING) ?: return InputResult.PROCESSED
        val last: Node = lastPaintedAnchor ?: run {
            lastPaintedAnchor = current
            return InputResult.PROCESSED
        }

        // One event can span several anchors when the cursor moves fast, so walk the lattice
        // between them and report every unit step rather than leaving a gap in the stroke.
        var cursor: Node = last
        puzzle.anchorPathBetween(last, current).forEach { next ->
            onSegmentListener?.onSegment(cursor.x, cursor.y, next.x, next.y)
            cursor = next
            paintedDuringDrag = true
        }
        lastPaintedAnchor = cursor
        return InputResult.PROCESSED
    }

    @Suppress("UnstableApiUsage")
    override fun onMouseUp(click: MouseButtonEvent): InputResult {
        if (mode != EditorMode.GRID) return InputResult.IGNORED

        val anchor: Node? = pressedAnchor
        val segment: Pair<Node, Node>? = pressedSegment
        val dragged: Boolean = paintedDuringDrag
        pressedAnchor = null
        pressedSegment = null
        lastPaintedAnchor = null
        paintedDuringDrag = false

        // A stroke has already reported itself step by step, so releasing it must not also fire a
        // click. A press that painted nothing acts on whatever it landed on.
        if (dragged) return InputResult.PROCESSED
        when {
            anchor != null -> onAnchorClickListener?.onAnchorClick(anchor.x, anchor.y)
            segment != null -> onSegmentListener?.onSegment(
                segment.first.x, segment.first.y, segment.second.x, segment.second.y
            )
        }
        return InputResult.PROCESSED
    }

    private fun outputPanel(): Panel? {
        val puzzleStack: ItemStack = inventory.getItem(outputSlotIndex)
        if (puzzleStack.isEmpty) return null
        return puzzleStack.panel
    }

    /**
     * The anchor nearest [click] within [padding] panel units on both axes, or null if the cursor
     * is not near one. Nearest rather than first: a click inside the tolerance of two anchors has
     * to resolve to the one actually under the cursor, or a stroke wanders off its own line.
     */
    private fun nearestAnchor(click: MouseButtonEvent, puzzle: Panel, padding: Float): Node? {
        val (x: Float, y: Float) = panelPosition(click, puzzle)
        return puzzle.anchors()
            .filter { anchor -> abs(anchor.x - x) <= padding && abs(anchor.y - y) <= padding }
            .minByOrNull { anchor -> (anchor.x - x) * (anchor.x - x) + (anchor.y - y) * (anchor.y - y) }
    }

    /** [click], which arrives in widget space, as a position in the panel's own coordinates. */
    private fun panelPosition(click: MouseButtonEvent, puzzle: Panel): Pair<Float, Float> {
        val scale: Int = maxOf(puzzle.width, puzzle.height)
        val x: Float = (1 - (click.x().toInt().toFloat() / width)) * scale
        val y: Float = (1 - (click.y().toInt().toFloat() / height)) * scale
        return x to y
    }

    @Suppress("UnstableApiUsage")
    override fun onClick(click: MouseButtonEvent, doubled: Boolean): InputResult {
        if (mode == EditorMode.GRID) return InputResult.PROCESSED

        val x: Int = click.x().toInt()
        val y: Int = click.y().toInt()
        val inputStack: ItemStack = inventory.getItem(outputSlotIndex)
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
