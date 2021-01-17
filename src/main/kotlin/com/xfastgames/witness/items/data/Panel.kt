package com.xfastgames.witness.items.data

import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.items.data.Panel.Companion.Type
import com.xfastgames.witness.utils.mutableGraph
import com.xfastgames.witness.utils.pow
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.DyeColor

private const val KEY_WIDTH = "width"
private const val KEY_HEIGHT = "height"
private const val KEY_LINE = "line"
private const val KEY_GRAPH = "graph"
private const val KEY_BACKGROUND_COLOR = "backgroundColor"
private const val KEY_PANEL_TYPE = "type"

@Suppress("UnstableApiUsage")
sealed class Panel(val type: Type) {
    abstract val line: ValueGraph<Node, Float>
    abstract val graph: ValueGraph<Node, Edge>
    abstract val backgroundColor: DyeColor
    abstract val width: Int
    abstract val height: Int

    abstract fun resize(length: Int): Panel

    data class Grid(
        override val line: ValueGraph<Node, Float>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
    ) : Panel(Type.Grid) {

        companion object {
            fun ofSize(size: Int): Grid = generatePanel(size, size)
            fun ofSize(width: Int, height: Int): Grid = generatePanel(width, height)

            @Suppress("UnstableApiUsage")
            fun generateGrid(width: Int, height: Int): ValueGraph<Node, Edge> {
                val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()

                val previousRow: MutableList<Node> = mutableListOf()
                repeat(width) { x ->
                    var previousNode: Node? = null
                    val currentRow: MutableList<Node> = mutableListOf()

                    repeat(height) { y ->
                        val (dx, dy) = x.toFloat() + 0.5f to y.toFloat() + 0.5f
                        val currentNode = Node(dx, dy)
                        graph.addNode(currentNode)
                        currentRow.add(currentNode)

                        // Link horizontal neighbour
                        previousNode?.let { node -> graph.putEdgeValue(node, currentNode, Modifier.NORMAL) }

                        // Link vertical neighbour
                        previousRow.takeIf { it.isNotEmpty() }
                            ?.let { row -> graph.putEdgeValue(row[y], currentNode, Modifier.NORMAL) }

                        previousNode = currentNode
                    }

                    previousNode = null
                    previousRow.clear()
                    previousRow.addAll(currentRow)
                    currentRow.clear()
                }
                return graph
            }

            private fun generatePanel(width: Int, height: Int): Grid {
                val graph: ValueGraph<Node, Edge> = generateGrid(width, height)
                val line: MutableValueGraph<Node, Float> = mutableGraph<Node, Float>().apply {
                    val startNode: Node? = graph.nodes().firstOrNull()
                    startNode?.let { addNode(startNode) }
                }

                return Grid(
                    line = line,
                    graph = graph,
                    backgroundColor = DyeColor.WHITE,
                    width = width,
                    height = height,
                )
            }
        }

        private fun grow(by: Int): Grid {
            if (by <= 0) return this
            // TODO Implement this
            return copy()
        }

        private fun shrink(by: Int): Grid {
            if (by <= 0) return this
            // TODO Implement this
            return copy()
        }

        override fun resize(length: Int): Grid =
            if (length > width) grow(length - width)
            else shrink(width - length)
    }

    data class Tree(
        override val line: ValueGraph<Node, Float>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
    ) : Panel(Type.Tree) {

        companion object {
            fun ofSize(height: Int): Tree = Tree(
                line = mutableGraph(),
                backgroundColor = DyeColor.WHITE,
                graph = generateTree(height),
                width = height,
                height = height
            )

            @Suppress("UnstableApiUsage")
            fun generateTree(size: Int): ValueGraph<Node, Edge> {
                val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
                val tree: MutableMap<Int, List<Node>> = mutableMapOf()
                (size downTo 0).forEach { branchIndex ->
                    val leaves: MutableList<Node> = mutableListOf()
                    val leafCount: Int = 2.pow(branchIndex)
                    repeat(leafCount) { leafIndex ->
                        val dY: Float = 1f * branchIndex
                        // centering has to happen here
                        val xOffset = (size.toFloat() / 2) / (branchIndex + 2)
                        val dX: Float = leafIndex * (size.toFloat() / leafCount) + xOffset
                        val leaf = Node(dX, dY)
                        graph.addNode(leaf)
                        leaves.add(leaf)
                        if (tree.isEmpty()) return@repeat // No need to connect branches on the top row
                        val branchAbove: Int = branchIndex + 1
                        val branch: List<Node> = tree[branchAbove]?.chunked(2)?.get(leafIndex) ?: return@repeat
                        branch.forEach { thatLeaf -> graph.putEdgeValue(thatLeaf, leaf, Modifier.NORMAL) }
                    }
                    tree[branchIndex] = leaves
                }
                return graph
            }
        }

        override fun resize(length: Int): Tree = TODO()
    }

    data class Freeform(
        override val line: ValueGraph<Node, Float>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
    ) : Panel(Type.Freeform) {
        override fun resize(length: Int): Freeform = TODO()
    }

    companion object {
        val DEFAULT: Panel by lazy { Grid.ofSize(3) }

        enum class Type { Grid, Tree, Freeform }
    }
}

@Suppress("UnstableApiUsage")
fun CompoundTag.getPanel(key: String): Panel? {
    if (!contains(key)) return null
    return getCompound(key).let { tag ->
        val type: Type = Type.values()[tag.getInt(KEY_PANEL_TYPE)]
        val line: ValueGraph<Node, Float> = tag.getFloatGraph(KEY_LINE)

        val backgroundColor: DyeColor = DyeColor.values()[tag.getInt(KEY_BACKGROUND_COLOR)]
        val grid: ValueGraph<Node, Edge> = tag.getEdgeGraph(KEY_GRAPH)

        when (type) {
            Type.Grid -> Panel.Grid(line, grid, backgroundColor, tag.getInt(KEY_WIDTH), tag.getInt(KEY_HEIGHT))
            Type.Tree -> Panel.Tree(line, grid, backgroundColor, tag.getInt(KEY_HEIGHT), tag.getInt(KEY_HEIGHT))
            Type.Freeform -> Panel.Freeform(line, grid, backgroundColor, tag.getInt(KEY_WIDTH), tag.getInt(KEY_HEIGHT))
        }
    }
}

fun CompoundTag.putPanel(key: String, panel: Panel) {
    put(key, CompoundTag().apply {
        putInt(KEY_PANEL_TYPE, panel.type.ordinal)
        putFloatGraph(KEY_LINE, panel.line)
        putInt(KEY_BACKGROUND_COLOR, panel.backgroundColor.ordinal)
        putEdgeGraph(KEY_GRAPH, panel.graph)
        when (panel) {
            is Panel.Grid, is Panel.Freeform -> {
                putInt(KEY_WIDTH, panel.width)
                putInt(KEY_HEIGHT, panel.height)
            }

            is Panel.Tree -> putInt(KEY_HEIGHT, panel.height)
        }
    })
}