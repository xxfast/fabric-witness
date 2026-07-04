package com.xfastgames.witness.items.renderer

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.incidentEdges
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import org.joml.Vector3f
import java.util.*
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

    private val lineFillTexture = Identifier.of(Witness.IDENTIFIER, "textures/entity/puzzle_panel_line_fill.png")
    private val solutionFillTexture =
        Identifier.of(Witness.IDENTIFIER, "textures/entity/puzzle_panel_solution_fill.png")

    private fun getBackdropTexture(color: DyeColor): Identifier =
        Identifier.of(
            Witness.IDENTIFIER,
            "textures/entity/puzzle_panel_backdrop_${color.name.lowercase(Locale.getDefault())}.png"
        )

    fun renderPanel(
        stack: ItemStack,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        val puzzle: Panel = stack.panel ?: Panel.DEFAULT
        renderPanel(puzzle, matrices, queue, light, overlay)
    }

    fun renderPanel(
        puzzle: Panel,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        renderBackground(puzzle.backgroundColor, matrices, queue, light, overlay)
        renderGraph(puzzle.graph, puzzle.width, puzzle.height, matrices, queue, light, overlay)
        renderLine(puzzle.line, puzzle.width, puzzle.height, matrices, queue, light, overlay)
    }

    fun renderBackground(
        dyeColor: DyeColor,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        val backdropTexture: Identifier = getBackdropTexture(dyeColor)
        queue.submitCustom(matrices, RenderLayers.beaconBeam(backdropTexture, false)) { entry, consumer ->
            consumer.square(entry, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)
        }
        matrices.pop()
    }

    private fun numberOfEdgesVisible(graph: ValueGraph<Node, Edge>, node: Node): Int =
        graph.incidentEdges(node).count { endpointPair ->
            graph.edgeValueOf(endpointPair) !in listOf(Edge.NONE, Edge.HIDDEN)
        }

    private fun RenderContext.renderNode(graph: ValueGraph<Node, Edge>, node: Node): Unit = when {
        node.modifier == Modifier.START -> circle(Vector3f(node.x, node.y, 0f), 4.pc)
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

        queue.submitCustom(matrices, RenderLayers.beaconBeam(lineFillTexture, false)) { entry, consumer ->
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

        queue.submitCustom(matrices, RenderLayers.beaconBeam(solutionFillTexture, false)) { entry, consumer ->
            withRenderContext(entry, consumer, light, overlay) {
                line.nodes().forEach { node ->
                    if (node.modifier == Modifier.START) circle(Vector3f(node.x, node.y, 0f), 4.pc)
                    else circle(Vector3f(node.x, node.y, 0f), 2.pc)
                }

                line.edges().forEach { side ->
                    val startNode: Node = side.nodeU()
                    val endNode: Node = side.nodeV()
                    val start = Vector3f(startNode.x, startNode.y, 0f)
                    val end = Vector3f(endNode.x, endNode.y, 0f)
                    edge(start, end, 4.pc, Modifier.NORMAL)
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

        fun RenderContext.start(
            start: Vector3f,
            end: Vector3f
        ) {
            line(start, end, thickness)
            val midpoint: Vector3f = (start + end) / 2f
            circle(midpoint, thickness)
        }

        when (edge) {
            Modifier.NONE -> {
            }
            Modifier.NORMAL -> line(start, end, thickness)
            Modifier.BREAK -> `break`(start, end)
            Modifier.DOT -> {
            }
            Modifier.START -> start(start, end)
            Modifier.END -> {
            }
            Modifier.HIDDEN -> {
            }
        }
    }
}
