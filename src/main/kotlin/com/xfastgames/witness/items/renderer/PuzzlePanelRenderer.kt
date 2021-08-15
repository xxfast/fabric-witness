package com.xfastgames.witness.items.renderer

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.ValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.incidentEdges
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.model.json.ModelTransformation.Mode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Arm
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.math.Matrix3f
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.math.Vec3f
import java.util.*
import kotlin.math.*

@Suppress("UnstableApiUsage")
object PuzzlePanelRenderer : BuiltinItemRendererRegistry.DynamicItemRenderer {

    private val paneStack = ItemStack(Items.BLACK_STAINED_GLASS_PANE, 1)
    private val lineFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_line_fill.png")
    private val solutionFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_solution_fill.png")

    private fun getBackdropTexture(color: DyeColor): Identifier =
        Identifier(
            Witness.IDENTIFIER,
            "textures/entity/puzzle_panel_backdrop_${color.name.lowercase(Locale.getDefault())}.png"
        )

    override fun render(
        stack: ItemStack,
        mode: Mode,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        when (mode) {
            Mode.FIRST_PERSON_LEFT_HAND -> {
                matrices.translate(-.5, .0, .0)
                matrices.push()
                matrices.scale(3.0f, 3.0f, 3.0f)
                matrices.translate(.0, -.5, -1.0)
                matrices.rotate(Vec3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.pop()
                renderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, Arm.LEFT)
            }

            Mode.FIRST_PERSON_RIGHT_HAND -> {
                matrices.translate(.5, .0, .0)
                matrices.push()
                matrices.scale(3.0f, 3.0f, 3.0f)
                matrices.translate(1.5, -.5, -1.0)
                matrices.rotate(Vec3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.pop()
                renderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, Arm.RIGHT)
            }

            Mode.GROUND, Mode.THIRD_PERSON_RIGHT_HAND, Mode.THIRD_PERSON_LEFT_HAND -> {
                val client: MinecraftClient = MinecraftClient.getInstance()
                matrices.translate(.5, .45, .5)
                client.itemRenderer.renderItem(paneStack, Mode.GROUND, light, overlay, matrices, vertexConsumers, 0)
                matrices.translate(.25, -.125, .02)
                matrices.scale(.5f, .5f, .5f)
                matrices.rotate(Vec3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.translate(1.0, .0, .075)
                matrices.rotate(Vec3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
            }

            else -> {
                matrices.translate(1.0, .0, .5)
                matrices.rotate(Vec3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
            }
        }
    }

    fun renderPanel(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        val puzzle: Panel = stack.nbt?.getPanel(KEY_PANEL) ?: Panel.DEFAULT
        renderBackground(puzzle.backgroundColor, matrices, vertexConsumers, light, overlay)
        renderGraph(puzzle.graph, puzzle.width, puzzle.height, matrices, vertexConsumers, light, overlay)
        renderLine(puzzle.line, puzzle.width, puzzle.height, matrices, vertexConsumers, light, overlay)
    }

    @Suppress("UnstableApiUsage")
    fun renderBackground(
        dyeColor: DyeColor,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        val backdropTexture: Identifier = getBackdropTexture(dyeColor)

        // TODO Figure out why entity coutout is making the lighting weird when rotated
        val backdropConsumer: VertexConsumer =
            vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(backdropTexture, false))

        backdropConsumer.square(matrices, Vec3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)
        return matrices.pop()
    }

    private fun numberOfEdgesVisible(graph: ValueGraph<Node, Edge>, node: Node): Int =
        graph.incidentEdges(node).count { endpointPair ->
            graph.edgeValueOf(endpointPair) !in listOf(Edge.NONE, Edge.HIDDEN)
        }

    private fun RenderContext.renderNode(graph: ValueGraph<Node, Edge>, node: Node): Unit = when {
        node.modifier == Modifier.START -> circle(Vec3f(node.x, node.y, 0f), 4.pc)
        numberOfEdgesVisible(graph, node) > 1 -> circle(Vec3f(node.x, node.y, 0f), 2.pc)
        numberOfEdgesVisible(graph, node) == 1 -> square(Vec3f(node.x - 2.pc, node.y - 2.pc, 0f), 4.pc)
        else -> Unit
    }

    private fun RenderContext.renderEdge(graph: ValueGraph<Node, Edge>, side: EndpointPair<Node>) {
        val edge: Edge = graph.edgeValueOf(side) ?: return
        val startNode: Node = side.nodeU()
        val endNode: Node = side.nodeV()
        val start = Vec3f(startNode.x, startNode.y, 0f)
        val end = Vec3f(endNode.x, endNode.y, 0f)
        edge(start, end, 4.pc, edge)
    }

    fun renderGraph(
        graph: ValueGraph<Node, Edge>,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        if (graph.nodes().isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.01)

        val fillConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(lineFillTexture, false))
        withRenderContext(matrices, fillConsumer, light, overlay) {
            graph.nodes().forEach { node -> renderNode(graph, node) }
            graph.edges().forEach { side -> renderEdge(graph, side) }
        }

        matrices.scale(1 + maxScale, 1 + maxScale, 1f)
        matrices.pop()
    }

    fun renderHighlighted(
        graph: ValueGraph<Node, Edge>,
        highlightedNode: Node? = null,
        highlightedEdge: EndpointPair<Node>? = null,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        vertexConsumer: VertexConsumer,
        light: Int,
        overlay: Int
    ) {
        if (graph.nodes().isEmpty()) return
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.push()
        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.01)

        if (highlightedNode != null || highlightedEdge != null) {
            withRenderContext(matrices, vertexConsumer, light, overlay) {
                highlightedNode?.let { node -> renderNode(graph, node) }
                highlightedEdge?.let { side -> renderEdge(graph, side) }
            }
        }

        matrices.scale(1 + maxScale, 1 + maxScale, 1f)
        matrices.pop()
    }

    @Suppress("UnstableApiUsage")
    fun renderLine(
        line: Graph<Node>,
        width: Int,
        height: Int,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()
        if (line.nodes().isEmpty()) return matrices.pop()
        val maxDimension: Int = maxOf(width, height)
        val maxScale: Float = 1f / maxDimension

        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.011)

        // Render line fill
        val fillConsumer: VertexConsumer =
            vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(solutionFillTexture, false))
        withRenderContext(matrices, fillConsumer, light, overlay) {
            line.nodes().forEach { node ->
                if (node.modifier == Modifier.START) circle(Vec3f(node.x, node.y, 0f), 4.pc)
                else circle(Vec3f(node.x, node.y, 0f), 2.pc)
            }

            line.edges().forEach { side ->
                val startNode: Node = side.nodeU()
                val endNode: Node = side.nodeV()
                val start = Vec3f(startNode.x, startNode.y, 0f)
                val end = Vec3f(endNode.x, endNode.y, 0f)
                edge(start, end, 4.pc, Modifier.NORMAL)
            }
        }

        matrices.scale(1 + maxScale, 1 + maxScale, 1f)

        return matrices.pop()
    }

    private fun RenderContext.edge(start: Vec3f, end: Vec3f, thickness: Float, edge: Edge) {
        fun RenderContext.`break`(
            start: Vec3f,
            end: Vec3f
        ) {
            val max: Vec3f = maxOf(start, end)
            val theta: Float = atan2(start.y - end.y, start.x - end.x)
            val halfThickness: Float = thickness / 2
            val lengthX: Float = start.x - end.x
            val lengthY: Float = start.y - end.y
            val length: Float = sqrt(lengthX.pow(2) + lengthY.pow(2)) + thickness
            val halfLength: Float = (length / 2)

            val vertices: List<Vec3f> = listOf(
                max.copy().apply { add(0f, -halfThickness, 0f) },
                max.copy().apply { add(0f, halfThickness, 0f) },
                max.copy().apply { add(halfLength - thickness, +halfThickness, 0f) },
                max.copy().apply { add(halfLength - thickness, -halfThickness, 0f) },
                max.copy().apply { add(halfLength, -halfThickness, 0f) },
                max.copy().apply { add(halfLength, +halfThickness, 0f) },
                max.copy().apply { add(length - thickness, +halfThickness, 0f) },
                max.copy().apply { add(length - thickness, -halfThickness, 0f) }
            ).map { corner ->
                val tempX: Float = corner.x - max.x
                val tempY: Float = corner.y - max.y
                val rotatedX: Float = tempX * cos(theta) - tempY * sin(theta)
                val rotatedY: Float = tempX * sin(theta) + tempY * cos(theta)
                Vec3f(rotatedX + max.x, rotatedY + max.y, max.z)
            }

            vertices.forEach { position ->
                val matrix: MatrixStack.Entry = matrices.peek()
                val normal: Matrix3f = matrix.normal
                val model: Matrix4f = matrix.model
                vertexConsumer.vertex(model, position.x, position.y, position.z)
                vertexConsumer.color(1f, 1f, 1f, 1f)
                vertexConsumer.texture(0f, 1f)
                vertexConsumer.overlay(overlay)
                vertexConsumer.light(light)
                vertexConsumer.normal(normal, .5f, .5f, .5f)
                vertexConsumer.next()
            }
        }

        fun RenderContext.start(
            start: Vec3f,
            end: Vec3f
        ) {
            line(start, end, thickness)
            val midpoint: Vec3f = (start + end) / 2f
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