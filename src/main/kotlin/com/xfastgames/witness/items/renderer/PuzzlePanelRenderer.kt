package com.xfastgames.witness.items.renderer

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.incidentEdges
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.DyeColor
import net.minecraft.util.math.BlockPos
import org.joml.Vector3f
import kotlin.math.*

/**
 * Renders puzzle panels in the world (puzzle frames / composer table).
 *
 * Migration notes (1.17 -> 1.21.11):
 * - The old `BuiltinItemRendererRegistry.DynamicItemRenderer` (custom in-hand / GUI / ground item
 *   rendering incl. the custom arm pose) was removed with the 1.21.4 item model rework. See
 *   TODO(migration) in [com.xfastgames.witness.items.PuzzlePanelItem].
 * - World rendering now goes through the ordered render command queue instead of a
 *   VertexConsumerProvider; geometry is submitted per render layer via `submitCustom`.
 */
@Suppress("UnstableApiUsage")
object PuzzlePanelRenderer {

    /**
     * Centre to point, so a hexagon is 3.pc across corners against the 4.pc line it marks. Narrower
     * than the line on purpose; see the open question in rules/witness/04-hexagon-dots.md.
     */
    private val HEXAGON_RADIUS: Float = 1.5f.pc

    fun renderPanel(
        stack: ItemStack,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int,
        framePos: BlockPos? = null,
    ) {
        val puzzle: Panel = stack.panel ?: Panel.DEFAULT
        renderPanel(puzzle, matrices, queue, light, overlay, framePos)
    }

    /**
     * @param framePos world position of the puzzle frame this panel is mounted on. Attract and
     * error flashes only draw when [framePos] matches the frame that owns the live effect, so a
     * reject never lights every tutorial panel nearby. Item / composer renders leave it null.
     */
    fun renderPanel(
        puzzle: Panel,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int,
        framePos: BlockPos? = null,
    ) {
        renderBackground(puzzle.backgroundColor, matrices, queue, light, overlay)
        renderGraph(puzzle.graph, puzzle.width, puzzle.height, matrices, queue, light, overlay)
        renderLine(puzzle.line, puzzle.width, puzzle.height, matrices, queue, light, overlay)
        // Symbols go in front of both, so a hexagon stays visible once the line covers it: that is
        // the only way a player can tell it was crossed (rules/witness/04-hexagon-dots.md).
        renderSymbols(
            puzzle.graph, puzzle.backgroundColor, puzzle.width, puzzle.height,
            matrices, queue, light, overlay
        )
        // Cue drawing is gated by the effect's own frame-pos match (set at trigger), not by
        // re-reading tutorial here — trigger already required tutorial, and a missing/stale
        // component must not silently drop an in-flight flash.
        if (framePos != null) {
            renderAttractPulse(puzzle, framePos, matrices, queue, light, overlay)
            renderErrorFlash(puzzle, framePos, matrices, queue, light, overlay)
        }
    }

    /**
     * Expanding white ring on start discs / end nubs ([PanelAttractPulse]). Translucent + emissive
     * so the ring stays white and fades via alpha (opaque dimming turned it grey/black).
     */
    private fun renderAttractPulse(
        puzzle: Panel,
        framePos: BlockPos,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        val frame: PanelAttractPulse.Frame = PanelAttractPulse.sample(framePos) ?: return
        val nodes: List<Node> = when (frame.kind) {
            PanelAttractPulse.Kind.START ->
                puzzle.graph.nodes().filter { it.modifier == Modifier.START }
            PanelAttractPulse.Kind.END ->
                puzzle.graph.nodes().filter { it.modifier == Modifier.END }
        }
        if (nodes.isEmpty()) return

        val baseRadius: Float = when (frame.kind) {
            PanelAttractPulse.Kind.START -> 4.pc
            PanelAttractPulse.Kind.END -> 2.pc
        }
        val t: Float = frame.progress
        val expand: Float = 1f - (1f - t) * (1f - t)
        val midRadius: Float = baseRadius * (0.30f + 1.25f * expand)
        val stroke: Float = baseRadius * 0.18f
        val innerRadius: Float = (midRadius - stroke).coerceAtLeast(0f)
        val outerRadius: Float = midRadius
        val alpha: Float = frame.strength * (1f - t) * (1f - t)
        if (alpha <= 0.02f) return

        val maxDimension: Int = maxOf(puzzle.width, puzzle.height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.014)

        // Fullbright translucent: white stays white, fade is pure alpha.
        val fullBright: Int = 0x00F000F0
        queue.submitCustom(
            matrices,
            RenderLayers.entityTranslucentEmissive(PuzzlePanelTextures.solutionFill)
        ) { entry, consumer ->
            withRenderContext(entry, consumer, fullBright, overlay) {
                nodes.forEach { node ->
                    ring(
                        Vector3f(node.x, node.y, 0f),
                        innerRadius,
                        outerRadius,
                        r = 1f,
                        g = 1f,
                        b = 1f,
                        a = alpha
                    )
                }
            }
        }

        matrices.pop()
    }

    /**
     * Red blink on missed hexagon dots ([PanelErrorFlash]). Same translucent-emissive path as the
     * attract ring so colour stays true red rather than a muddy opaque blend.
     */
    private fun renderErrorFlash(
        puzzle: Panel,
        framePos: BlockPos,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        val frame: PanelErrorFlash.Frame = PanelErrorFlash.sample(framePos) ?: return
        if (frame.alpha <= 0.02f || frame.positions.isEmpty()) return

        val maxDimension: Int = maxOf(puzzle.width, puzzle.height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.015)

        val fullBright: Int = 0x00F000F0
        queue.submitCustom(
            matrices,
            RenderLayers.entityTranslucentEmissive(PuzzlePanelTextures.solutionFill)
        ) { entry, consumer ->
            withRenderContext(entry, consumer, fullBright, overlay) {
                frame.positions.forEach { (x, y) ->
                    hexagon(
                        Vector3f(x, y, 0f),
                        HEXAGON_RADIUS,
                        r = 1f,
                        g = 0.12f,
                        b = 0.08f,
                        a = frame.alpha
                    )
                }
            }
        }

        matrices.pop()
    }

    /**
     * Draws each hexagon in the panel's backdrop colour, so it reads as a notch punched through
     * whatever covers it.
     *
     * A hexagon is drawn narrower than the line it marks, since at full width it would sever the
     * line and read as a broken edge. See the open question in rules/witness/04-hexagon-dots.md:
     * this is not settled, and it is the one number to change when it is.
     */
    fun renderSymbols(
        graph: ValueGraph<Node, Edge>,
        backgroundColor: DyeColor,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        val hexagons: List<Vector3f> = symbolPositions(graph)
        if (hexagons.isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.012)

        val backdrop = PuzzlePanelTextures.backdrop(backgroundColor)
        queue.submitCustom(matrices, RenderLayers.beaconBeam(backdrop, false)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                hexagons.forEach { position -> hexagon(position, HEXAGON_RADIUS) }
            }
        }

        matrices.pop()
    }

    /** Where every hexagon on [graph] sits: on its node, or at the midpoint of its edge. */
    private fun symbolPositions(graph: ValueGraph<Node, Edge>): List<Vector3f> {
        val nodes: List<Vector3f> = graph.nodes()
            .filter { node -> node.symbol == Symbol.HEXAGON }
            .map { node -> Vector3f(node.x, node.y, 0f) }
        val edges: List<Vector3f> = graph.edges()
            .filter { side -> graph.edgeValueOf(side)?.symbol == Symbol.HEXAGON }
            .map { side ->
                val u: Node = side.nodeU()
                val v: Node = side.nodeV()
                Vector3f((u.x + v.x) / 2, (u.y + v.y) / 2, 0f)
            }
        return nodes + edges
    }

    fun renderBackground(
        dyeColor: DyeColor,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        val backdropTexture = PuzzlePanelTextures.backdrop(dyeColor)
        queue.submitCustom(matrices, RenderLayers.beaconBeam(backdropTexture, false)) { entry, consumer ->
            consumer.square(entry, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)
        }
        matrices.pop()
    }

    private fun numberOfEdgesVisible(graph: ValueGraph<Node, Edge>, node: Node): Int =
        graph.incidentEdges(node).count { endpointPair ->
            graph.edgeValueOf(endpointPair)?.modifier !in listOf(Modifier.NONE, Modifier.HIDDEN)
        }

    private fun RenderContext.renderNode(graph: ValueGraph<Node, Edge>, node: Node): Unit = when {
        node.modifier == Modifier.START -> circle(Vector3f(node.x, node.y, 0f), 4.pc)
        // The nub's edge is drawn like any other; the node just rounds off its tip.
        node.modifier == Modifier.END -> circle(Vector3f(node.x, node.y, 0f), 2.pc)
        numberOfEdgesVisible(graph, node) > 1 -> circle(Vector3f(node.x, node.y, 0f), 2.pc)
        numberOfEdgesVisible(graph, node) == 1 -> square(Vector3f(node.x - 2.pc, node.y - 2.pc, 0f), 4.pc)
        else -> Unit
    }

    private fun RenderContext.renderEdge(graph: ValueGraph<Node, Edge>, side: EndpointPair<Node>) {
        val edge: Edge = graph.edgeValueOf(side) ?: return
        val startNode: Node = side.nodeU()
        val endNode: Node = side.nodeV()
        val start = Vector3f(startNode.x, startNode.y, 0f)
        val end = Vector3f(endNode.x, endNode.y, 0f)
        edge(start, end, 4.pc, edge)
    }

    fun renderGraph(
        graph: ValueGraph<Node, Edge>,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        if (graph.nodes().isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.01)

        queue.submitCustom(matrices, RenderLayers.beaconBeam(PuzzlePanelTextures.lineFill, false)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                graph.nodes().forEach { node -> renderNode(graph, node) }
                graph.edges().forEach { side -> renderEdge(graph, side) }
            }
        }

        matrices.pop()
    }

    fun renderLine(
        line: Graph<Node>,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        if (line.nodes().isEmpty()) return matrices.pop()
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.011)

        queue.submitCustom(matrices, RenderLayers.beaconBeam(PuzzlePanelTextures.solutionFill, false)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                line.nodes().forEach { node ->
                    // Only the start point the line was picked up from fills its whole circle. A
                    // start the line merely travels over keeps its own disc and is covered by the
                    // line width alone, so it stays visible either side of the line.
                    val pickedUp: Boolean = node.modifier == Modifier.START && line.degree(node) <= 1
                    circle(Vector3f(node.x, node.y, 0f), if (pickedUp) 4.pc else 2.pc)
                }

                line.edges().forEach { side ->
                    val startNode: Node = side.nodeU()
                    val endNode: Node = side.nodeV()
                    val start = Vector3f(startNode.x, startNode.y, 0f)
                    val end = Vector3f(endNode.x, endNode.y, 0f)
                    edge(start, end, 4.pc, Edge.NORMAL)
                }
            }
        }

        return matrices.pop()
    }

    private fun RenderContext.edge(start: Vector3f, end: Vector3f, thickness: Float, edge: Edge) {
        fun RenderContext.`break`(
            start: Vector3f,
            end: Vector3f
        ) {
            val max: Vector3f = maxOf(start, end)
            val theta: Float = atan2(start.y - end.y, start.x - end.x)
            val halfThickness: Float = thickness / 2
            val lengthX: Float = start.x - end.x
            val lengthY: Float = start.y - end.y
            val length: Float = sqrt(lengthX.pow(2) + lengthY.pow(2)) + thickness
            val halfLength: Float = (length / 2)

            val vertices: List<Vector3f> = listOf(
                Vector3f(max).add(0f, -halfThickness, 0f),
                Vector3f(max).add(0f, halfThickness, 0f),
                Vector3f(max).add(halfLength - thickness, +halfThickness, 0f),
                Vector3f(max).add(halfLength - thickness, -halfThickness, 0f),
                Vector3f(max).add(halfLength, -halfThickness, 0f),
                Vector3f(max).add(halfLength, +halfThickness, 0f),
                Vector3f(max).add(length - thickness, +halfThickness, 0f),
                Vector3f(max).add(length - thickness, -halfThickness, 0f)
            ).map { corner ->
                val tempX: Float = corner.x - max.x
                val tempY: Float = corner.y - max.y
                val rotatedX: Float = tempX * cos(theta) - tempY * sin(theta)
                val rotatedY: Float = tempX * sin(theta) + tempY * cos(theta)
                Vector3f(rotatedX + max.x, rotatedY + max.y, max.z)
            }

            vertices.forEach { position ->
                vertexConsumer.vertex(entry.positionMatrix, position.x, position.y, position.z)
                    .color(1f, 1f, 1f, 1f)
                    .texture(0f, 1f)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry, .5f, .5f, .5f)
            }
        }

        when (edge.modifier) {
            Modifier.NONE -> {
            }
            // A start point is a node role, never an edge value. A legacy panel that stored one on
            // an edge draws as a plain segment: the disc goes, the segment stays, and it traces
            // like any other (rules/witness/01-start-points.md).
            Modifier.NORMAL, Modifier.START -> line(start, end, thickness)
            Modifier.BREAK -> `break`(start, end)
            Modifier.DOT -> {
            }
            Modifier.END -> {
            }
            Modifier.HIDDEN -> {
            }
        }
    }
}
