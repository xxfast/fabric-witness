package com.xfastgames.witness.screens.widgets

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.xfastgames.witness.items.KEY_PANEL
import com.xfastgames.witness.items.data.*
import com.xfastgames.witness.items.renderer.PuzzlePanelRenderer
import com.xfastgames.witness.utils.intersects
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

private const val CLICK_PADDING = 0.2f

class WPuzzleEditor(
    private val inventory: Inventory,
    private val outputSlotIndex: Int
) : WWidget() {

    fun interface OnClickListener {
        fun onClick(updatedPuzzle: Panel)
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
        backgroundPainter.paintBackground(x, y, this)
        matrices.push()
        val puzzleStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (puzzleStack.isEmpty) return matrices.pop()

        /** TODO: Figure out why the puzzle background is not rendered in the render pass
         * Also, 🎩🐇 magic number land!
         */
        val immediateConsumer: VertexConsumerProvider.Immediate = client.bufferBuilders.entityVertexConsumers
        val puzzleScale = 6.75f
        matrices.scale(puzzleScale, -puzzleScale, puzzleScale)
        matrices.rotate(Vector3f.POSITIVE_Z, 180f)
        // Translate relative to panel placement
        matrices.translate(-1.275, -.895, .0)

        val puzzle: Panel = puzzleStack.tag?.getPanel(KEY_PANEL) ?: Panel.DEFAULT

        puzzlePanelRenderer.renderGraph(
            puzzle.graph,
            puzzle.width,
            puzzle.height,
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

    @Suppress("UnstableApiUsage")
    override fun onClick(x: Int, y: Int, button: Int) {
        val inputStack: ItemStack = inventory.getStack(outputSlotIndex)
        if (inputStack.isEmpty) return
        val inputPuzzle: Panel = inputStack.tag?.getPanel(KEY_PANEL) ?: return

        val xPosition = 1 - (x.toFloat() / width)
        val yPosition = 1 - (y.toFloat() / height)

        val puzzleRelativeX: Float = xPosition * inputPuzzle.width
        val puzzleRelativeY: Float = yPosition * inputPuzzle.height

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

        val updatedNode: Node? =
            node?.copy(modifier = if (node.modifier == Modifier.START) Modifier.NONE else Modifier.START)

        val updatedGraph: MutableValueGraph<Node, Edge> = Graphs.copyOf(inputPuzzle.graph)

        updatedNode?.let {
            val neighbours: List<Node> = inputPuzzle.graph.adjacentNodes(node).toList()
            val neighbourhood: MutableMap<Node, Edge> = mutableMapOf()
            neighbours.forEach { neighbour ->
                neighbourhood[neighbour] = inputPuzzle.graph.edgeValue(neighbour, node).get()
            }
            updatedGraph.removeNode(node)
            updatedGraph.addNode(updatedNode)
            neighbourhood.forEach { (neighbour, edge) ->
                updatedGraph.putEdgeValue(neighbour, updatedNode, edge)
            }
        }

        val updatedEdge: Edge? = edge?.nextIn(Modifier.NORMAL, Modifier.START, Modifier.BREAK, Modifier.HIDDEN)
        if (updatedNode == null && updatedEdge != null) {
            updatedGraph.removeEdge(edgeNodePair.nodeU(), edgeNodePair.nodeV())
            updatedGraph.putEdgeValue(edgeNodePair, updatedEdge)
        }

        // TODO: Do this nicely 😅💩
        val updatedPuzzle: Panel = when (inputPuzzle) {
            is Panel.Grid -> inputPuzzle.copy(graph = updatedGraph)
            is Panel.Tree -> inputPuzzle.copy(graph = updatedGraph)
            is Panel.Freeform -> inputPuzzle.copy(graph = updatedGraph)
        }

        if (updatedPuzzle == inputPuzzle) return
        onClickListener?.onClick(updatedPuzzle)
    }
}