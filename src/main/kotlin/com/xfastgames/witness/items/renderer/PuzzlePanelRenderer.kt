package com.xfastgames.witness.items.renderer

import com.google.common.graph.ValueGraph
import com.xfastgames.witness.Witness
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.utils.*
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.model.json.ModelTransformation.Mode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.util.math.Vector3f
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Arm
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.math.Matrix3f
import net.minecraft.util.math.Matrix4f
import kotlin.math.*

object PuzzlePanelRenderer : BuiltinItemRendererRegistry.DynamicItemRenderer {

    private val paneStack = ItemStack(Items.BLACK_STAINED_GLASS_PANE, 1)
    private val lineFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_line_fill.png")
    private val solutionFillTexture = Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_solution_fill.png")

    private fun getBackdropTexture(color: DyeColor): Identifier =
        Identifier(Witness.IDENTIFIER, "textures/entity/puzzle_panel_backdrop_${color.name.toLowerCase()}.png")

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
                matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.pop()
                renderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, Arm.LEFT)
            }

            Mode.FIRST_PERSON_RIGHT_HAND -> {
                matrices.translate(.5, .0, .0)
                matrices.push()
                matrices.scale(3.0f, 3.0f, 3.0f)
                matrices.translate(1.5, -.5, -1.0)
                matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.pop()
                renderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, Arm.RIGHT)
            }

            Mode.GROUND, Mode.THIRD_PERSON_RIGHT_HAND, Mode.THIRD_PERSON_LEFT_HAND -> {
                val client: MinecraftClient = MinecraftClient.getInstance()
                matrices.translate(.5, .45, .5)
                client.itemRenderer.renderItem(paneStack, Mode.GROUND, light, overlay, matrices, vertexConsumers)
                matrices.translate(.25, -.125, .02)
                matrices.scale(.5f, .5f, .5f)
                matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
                matrices.translate(1.0, .0, .075)
                matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
            }

            else -> {
                matrices.translate(1.0, .0, .5)
                matrices.rotate(Vector3f.NEGATIVE_Y, 180f)
                renderPanel(stack, matrices, vertexConsumers, light, overlay)
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun renderPanel(
        stack: ItemStack,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        // Retrieve panel to render
        // If there's no tag
        val puzzle: Panel = stack.tag?.getPanel() ?: Panel.DEFAULT

        // Render Panel background
        val backdropTexture: Identifier = getBackdropTexture(puzzle.backgroundColor)

        // TODO Figure out why entity coutout is making the lighting weird when rotated
        val backdropConsumer: VertexConsumer =
            vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(backdropTexture, false))

        backdropConsumer.square(matrices, Vector3f(0.pc, 0.pc, 0.pc), 16.pc, light, overlay)

        // Scale items to fit on frame
        if (puzzle.graph.nodes().isEmpty()) return matrices.pop()

        val width = puzzle.width
        val height = puzzle.height

        val xDelta: Float = (puzzle.graph.nodes().maxOf { it.x } - puzzle.graph.nodes().minOf { it.x })
        val yDelta: Float = (puzzle.graph.nodes().maxOf { it.y } - puzzle.graph.nodes().minOf { it.y })

        val maxDelta: Float = maxOf(xDelta, yDelta)

        val xScale: Float = 1f / width
        val yScale: Float = 1f / height

        // Leave one tile for padding
        val maxScale: Float = 1f / (maxDelta + 1)

        matrices.scale(maxScale, maxScale, 1f)
        matrices.translate(.0, .0, -.01)

        // Render grid vertices
        val fillConsumer1: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(lineFillTexture, false))
        val graph: ValueGraph<Node, Edge> = puzzle.graph
        withRenderContext(matrices, fillConsumer1, light, overlay) {
            graph.nodes().forEach { node ->
                val isThereAnyVisibleEdges: Boolean = graph.incidentEdges(node).any { endpointPair ->
                    graph.edgeValue(endpointPair).value !in listOf(Edge.NONE, Edge.HIDDEN)
                }

                if (node.modifier == Modifier.START) circle(Vector3f(node.x, node.y, 0f), 4.pc)
                else if (isThereAnyVisibleEdges) circle(Vector3f(node.x, node.y, 0f), 2.pc)
            }

            graph.edges().forEach { side ->
                val edge: Edge = graph.edgeValue(side).value ?: return@forEach
                val startNode: Node = side.nodeU()
                val endNode: Node = side.nodeV()
                val start = Vector3f(startNode.x, startNode.y, 0f)
                val end = Vector3f(endNode.x, endNode.y, 0f)
                // TODO: Render the correct line
                edge(start, end, 4.pc, edge)
            }
        }

        matrices.scale(1 + maxScale, 1 + maxScale, 1f)
        matrices.pop()
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
                Vector3f(rotatedX + max.x, rotatedY + max.y, max.z)
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

    private fun renderTile(
        tile: Tile,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int
    ) {
        matrices.push()

        val position = Vector3f(0f, 0f, -0.001f)

        // Render tile pieces
        val fillConsumer: VertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(lineFillTexture))

        // Render tile lines
        withRenderContext(matrices, fillConsumer, light, overlay) {
            tile.lines.forEach { (direction, side) ->
                when (direction) {
                    Direction.TOP -> when (side) {
                        Line.FILLED -> rectangle(Vector3f(6.pc, 0.pc, position.z), 4.pc, 6.pc)
                        Line.SHORTENED -> rectangle(Vector3f(6.pc, 2.pc, position.z), 4.pc, 4.pc)
                        Line.END -> {
                            circle(Vector3f(8.pc, 4.pc, position.z), 2.pc, arc = 90..270)
                            rectangle(Vector3f(6.pc, 4.pc, position.z), 4.pc, 2.pc)
                        }
                    }

                    Direction.RIGHT -> when (side) {
                        Line.FILLED -> rectangle(Vector3f(10.pc, 6.pc, position.z), 6.pc, 4.pc)
                        Line.SHORTENED -> rectangle(Vector3f(10.pc, 6.pc, position.z), 4.pc, 4.pc)
                        Line.END -> {
                            circle(Vector3f(12.pc, 8.pc, position.z), 2.pc, 0..180)
                            rectangle(Vector3f(10.pc, 6.pc, position.z), 2.pc, 4.pc)
                        }
                    }

                    Direction.BOTTOM -> when (side) {
                        Line.FILLED -> rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 6.pc)
                        Line.SHORTENED -> rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 4.pc)
                        Line.END -> {
                            circle(Vector3f(8.pc, 12.pc, position.z), 2.pc, arc = -90..90)
                            rectangle(Vector3f(6.pc, 10.pc, position.z), 4.pc, 2.pc)
                        }
                    }

                    Direction.LEFT -> when (side) {
                        Line.FILLED -> rectangle(Vector3f(0.pc, 6.pc, position.z), 6.pc, 4.pc)
                        Line.SHORTENED -> rectangle(Vector3f(2.pc, 6.pc, position.z), 4.pc, 4.pc)
                        Line.END -> {
                            circle(Vector3f(4.pc, 8.pc, position.z), 2.pc, 180..360)
                            rectangle(Vector3f(4.pc, 6.pc, position.z), 2.pc, 4.pc)
                        }
                    }
                }
            }

            // Render tile centers or the vertex
            if (tile.isStart) circle(Vector3f(8.pc, 8.pc, position.z), 5.pc)
            else when {
                tile.center.containsOnly(Direction.TOP, Direction.LEFT) -> {
                    square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                    circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 0..90)
                }

                tile.center.containsOnly(Direction.TOP, Direction.RIGHT) -> {
                    square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                    circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 270..360)
                }

                tile.center.containsOnly(Direction.BOTTOM, Direction.LEFT) -> {
                    square(Vector3f(6.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                    square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                    circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 90..180)
                }

                tile.center.containsOnly(Direction.BOTTOM, Direction.RIGHT) -> {
                    square(Vector3f(8.pc, 6.pc, position.z), 2.pc)
                    square(Vector3f(6.pc, 8.pc, position.z), 2.pc)
                    square(Vector3f(8.pc, 8.pc, position.z), 2.pc)
                    circle(Vector3f(8.pc, 8.pc, position.z), 2.pc, 180..270)
                }

                tile.center.isEmpty() -> {
                }

                else -> square(Vector3f(6.pc, 6.pc, position.z), 4.pc)
            }
        }
        matrices.pop()
    }
}