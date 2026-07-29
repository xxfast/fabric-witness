package com.xfastgames.witness.items.data

import com.google.common.graph.Graph
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.items.data.Panel.Companion.Type
import com.xfastgames.witness.utils.guava.emptyGraph
import com.xfastgames.witness.utils.guava.mutableGraph
import com.xfastgames.witness.utils.pow
import com.mojang.serialization.Codec
import com.xfastgames.witness.utils.getIntTolerant
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.util.DyeColor
import kotlin.math.roundToInt

private const val KEY_WIDTH = "width"
private const val KEY_HEIGHT = "height"
private const val KEY_LINE = "line"
private const val KEY_GRAPH = "graph"
private const val KEY_BACKGROUND_COLOR = "backgroundColor"
private const val KEY_PANEL_TYPE = "type"

@Suppress("UnstableApiUsage")
sealed class Panel(val type: Type) {
    abstract val line: Graph<Node>
    abstract val graph: ValueGraph<Node, Edge>
    abstract val backgroundColor: DyeColor
    abstract val width: Int
    abstract val height: Int

    abstract fun resize(length: Int): Panel

    data class Grid(
        override val line: Graph<Node>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
    ) : Panel(Type.Grid) {

        companion object {
            /**
             * Per-axis ceiling in nodes, which is 8 cells. Bounded by what stays legible in the
             * solver, what fits in one stack when recycled, and what is sane to sync on an itemstack.
             */
            const val MAX_NODES: Int = 9

            fun ofSize(size: Int): Grid = generatePanel(size, size)
            fun ofSize(width: Int, height: Int): Grid = generatePanel(width, height)

            /**
             * Distance from the origin to node index `0` on each axis. A grid is centred inside the
             * square its longest side describes, so the offsets change with the aspect ratio and any
             * copy between two sizes has to go through them rather than reuse raw coordinates.
             */
            fun gridOffsets(width: Int, height: Int): Pair<Float, Float> {
                var xOffset = 0.5f
                if (width < height) xOffset += 0.5f * (height - width)
                var yOffset = 0.5f
                if (height < width) yOffset += 0.5f * (width - height)
                return xOffset to yOffset
            }

            @Suppress("UnstableApiUsage")
            fun generateGrid(width: Int, height: Int): ValueGraph<Node, Edge> {
                val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()

                val (xOffset: Float, yOffset: Float) = gridOffsets(width, height)

                val previousRow: MutableList<Node> = mutableListOf()
                repeat(width) { x ->
                    var previousNode: Node? = null
                    val currentRow: MutableList<Node> = mutableListOf()

                    repeat(height) { y ->
                        val (dx, dy) = x.toFloat() + xOffset to y.toFloat() + yOffset
                        val currentNode = Node(dx, dy)
                        graph.addNode(currentNode)
                        currentRow.add(currentNode)

                        // Link horizontal neighbour
                        previousNode?.let { node -> graph.putEdgeValue(node, currentNode, Edge.NORMAL) }

                        // Link vertical neighbour
                        previousRow.takeIf { it.isNotEmpty() }
                            ?.let { row -> graph.putEdgeValue(row[y], currentNode, Edge.NORMAL) }

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
                val line: Graph<Node> = emptyGraph()

                return Grid(
                    line = line,
                    graph = graph,
                    backgroundColor = DyeColor.WHITE,
                    width = width,
                    height = height,
                )
            }
        }

        /**
         * A copy [width] × [height] nodes in size, with this panel's nodes and edges transplanted
         * into it at index offset ([offsetX], [offsetY]). Modifiers survive, so growing a composed
         * puzzle keeps its start points, ends, hexagons and broken edges; the drawn [line] does not,
         * since a partial line on a resized grid is meaningless.
         *
         * Node index `(0, 0)` is the low corner of both axes: `+x` runs right and `+y` runs up on the
         * rendered panel, so a caller working from the crafting grid (whose y runs *down*) has to flip
         * the y offset.
         */
        @Suppress("UnstableApiUsage")
        fun expandTo(width: Int, height: Int, offsetX: Int = 0, offsetY: Int = 0): Grid {
            if (width < this.width || height < this.height) return this
            if (width == this.width && height == this.height && offsetX == 0 && offsetY == 0) return this

            val (sourceXOffset: Float, sourceYOffset: Float) = gridOffsets(this.width, this.height)
            val (targetXOffset: Float, targetYOffset: Float) = gridOffsets(width, height)

            val sourceNodes: Map<Pair<Int, Int>, Node> = graph.nodes().associateBy { node ->
                (node.x - sourceXOffset).roundToInt() to (node.y - sourceYOffset).roundToInt()
            }

            /** The source node that ends up at target index ([x], [y]), if any. */
            fun sourceAt(x: Int, y: Int): Node? = sourceNodes[x - offsetX to y - offsetY]

            val target: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
            val targetNodes: MutableMap<Pair<Int, Int>, Node> = mutableMapOf()

            repeat(width) { x ->
                repeat(height) { y ->
                    // Both halves of the source node travel: dropping the symbol here would
                    // silently strip a panel's hexagons the moment its grid is grown.
                    val source: Node? = sourceAt(x, y)
                    val node = Node(
                        x = x + targetXOffset,
                        y = y + targetYOffset,
                        modifier = source?.modifier ?: Modifier.NONE,
                        symbol = source?.symbol ?: Symbol.NONE
                    )
                    targetNodes[x to y] = node
                    target.addNode(node)
                }
            }

            repeat(width) { x ->
                repeat(height) { y ->
                    val node: Node = targetNodes.getValue(x to y)
                    listOf(x - 1 to y, x to y - 1).forEach { neighbourIndex ->
                        val neighbour: Node = targetNodes[neighbourIndex] ?: return@forEach
                        val from: Node? = sourceAt(x, y)
                        val to: Node? = sourceAt(neighbourIndex.first, neighbourIndex.second)
                        val edge: Edge = when {
                            // Both ends came from the source: carry its edge over, and honour a
                            // deliberately missing one by leaving the pair unconnected.
                            from != null && to != null ->
                                graph.edgeValue(from, to).orElse(null) ?: return@forEach

                            else -> Edge.NORMAL
                        }
                        target.putEdgeValue(neighbour, node, edge)
                    }
                }
            }

            return copy(line = emptyGraph(), graph = target, width = width, height = height)
        }

        private fun grow(by: Int): Grid {
            if (by <= 0) return this
            return expandTo(width + by, height + by)
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
        override val line: Graph<Node>,
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
                        branch.forEach { thatLeaf -> graph.putEdgeValue(thatLeaf, leaf, Edge.NORMAL) }
                    }
                    tree[branchIndex] = leaves
                }
                return graph
            }
        }

        override fun resize(length: Int): Tree = TODO()
    }

    data class Freeform(
        override val line: Graph<Node>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
    ) : Panel(Type.Freeform) {
        override fun resize(length: Int): Freeform = TODO()
    }

    companion object {
        /**
         * Codec used by the `witness:panel` data component. It reuses the existing NBT
         * (de)serialization so the stored shape matches the pre-1.20.5 raw-NBT `panel` tag,
         * and recipe JSON can specify the component with the same structure.
         */
        val CODEC: Codec<Panel> = NbtCompound.CODEC.xmap(
            { nbt -> nbt.toPanel() },
            { panel -> panel.toNbt() }
        )

        val PACKET_CODEC: PacketCodec<RegistryByteBuf, Panel> = PacketCodecs.registryCodec(CODEC)

        val DEFAULT: Panel by lazy { Grid.ofSize(3) }

        val TEST: Panel by lazy {
            val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder
                .undirected()
                .build()

            val bottomRight = Node(0.5f, 0.5f, Modifier.START)
            val topLeft = Node(2.5f, 2.5f, Modifier.NORMAL)
            graph.addNode(bottomRight)
            graph.addNode(topLeft)
            graph.putEdgeValue(bottomRight, topLeft, Edge.NORMAL)

            Freeform(
                line = emptyGraph(),
                graph = graph,
                backgroundColor = DyeColor.CYAN,
                width = 3,
                height = 3
            )
        }

        enum class Type { Grid, Tree, Freeform }
    }
}

/** Reads a panel whose fields are stored at the root of this compound. */
@Suppress("UnstableApiUsage")
fun NbtCompound.toPanel(): Panel {
    val type: Type = Type.values()[getIntTolerant(KEY_PANEL_TYPE)]
    val line: Graph<Node> = getGraph(KEY_LINE)

    val backgroundColor: DyeColor = DyeColor.values()[getIntTolerant(KEY_BACKGROUND_COLOR)]
    val grid: ValueGraph<Node, Edge> = getValueGraph(KEY_GRAPH)

    return when (type) {
        Type.Grid -> Panel.Grid(line, grid, backgroundColor, getIntTolerant(KEY_WIDTH), getIntTolerant(KEY_HEIGHT))
        Type.Tree -> Panel.Tree(line, grid, backgroundColor, getIntTolerant(KEY_HEIGHT), getIntTolerant(KEY_HEIGHT))
        Type.Freeform -> Panel.Freeform(line, grid, backgroundColor, getIntTolerant(KEY_WIDTH), getIntTolerant(KEY_HEIGHT))
    }
}

/** Writes this panel's fields into a fresh compound (inverse of [toPanel]). */
fun Panel.toNbt(): NbtCompound = NbtCompound().also { tag ->
    tag.putInt(KEY_PANEL_TYPE, type.ordinal)
    tag.putGraph(KEY_LINE, line)
    tag.putInt(KEY_BACKGROUND_COLOR, backgroundColor.ordinal)
    tag.putValueGraph(KEY_GRAPH, graph)
    when (this) {
        is Panel.Grid, is Panel.Freeform -> {
            tag.putInt(KEY_WIDTH, width)
            tag.putInt(KEY_HEIGHT, height)
        }

        is Panel.Tree -> tag.putInt(KEY_HEIGHT, height)
    }
}

@Suppress("UnstableApiUsage")
fun NbtCompound.getPanel(key: String): Panel? {
    if (!contains(key)) return null
    return getCompoundOrEmpty(key).toPanel()
}

fun NbtCompound.putPanel(key: String, panel: Panel) {
    put(key, panel.toNbt())
}