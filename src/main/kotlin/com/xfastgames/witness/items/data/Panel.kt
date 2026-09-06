package com.xfastgames.witness.items.data

import com.google.common.graph.Graph
import com.google.common.graph.Graphs
import com.google.common.graph.MutableValueGraph
import com.google.common.graph.ValueGraph
import com.google.common.graph.ValueGraphBuilder
import com.xfastgames.witness.items.data.Panel.Companion.Type
import com.xfastgames.witness.utils.guava.edgeValueOf
import com.xfastgames.witness.utils.guava.emptyGraph
import com.xfastgames.witness.utils.pow
import com.mojang.serialization.Codec
import com.xfastgames.witness.utils.getBooleanTolerant
import com.xfastgames.witness.utils.getIntTolerant
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.item.DyeColor
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

private const val KEY_WIDTH = "width"
private const val KEY_HEIGHT = "height"
private const val KEY_LINE = "line"
private const val KEY_GRAPH = "graph"
private const val KEY_BACKGROUND_COLOR = "backgroundColor"
private const val KEY_PANEL_TYPE = "type"
private const val KEY_TUTORIAL = "tutorial"
private const val KEY_SYMBOLS = "symbols"
private const val KEY_LEVELS = "levels"

@Suppress("UnstableApiUsage")
sealed class Panel(val type: Type) {
    abstract val line: Graph<Node>
    abstract val graph: ValueGraph<Node, Edge>
    abstract val backgroundColor: DyeColor
    abstract val width: Int
    abstract val height: Int
    /** When true, this panel is authored as a tutorial panel (composer flag; no runtime effect yet). */
    abstract val tutorial: Boolean
    /** Region symbols, one per occupied cell (rules/witness/06-colored-squares.md). */
    abstract val symbols: List<CellSymbol>

    abstract fun resize(length: Int): Panel

    data class Grid(
        override val line: Graph<Node>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
        override val tutorial: Boolean = false,
        override val symbols: List<CellSymbol> = emptyList(),
    ) : Panel(Type.Grid) {

        companion object {
            /**
             * Per-axis ceiling in nodes, which is 10 cells, the largest grid The Witness itself uses.
             * Range by what stays legible in the solver, what fits in one stack when recycled, and
             * what is sane to sync on an itemstack.
             */
            const val MAX_NODES: Int = 11

            fun ofSize(size: Int): Grid = generatePanel(size, size)
            fun ofSize(width: Int, height: Int): Grid = generatePanel(width, height)

            /**
             * DistancePerDirection from the origin to node index `0` on each axis. A grid is centred inside the
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
                        symbol = source?.symbol ?: Atom.NONE
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

            // Cell centres are panel-unit positions, and the lattice recentres when the aspect
            // ratio changes, so a symbol goes through the same offset arithmetic as the nodes.
            // Copying its coordinates raw would silently shift every square on a grow.
            val movedSymbols: List<CellSymbol> = symbols.map { symbol ->
                symbol.copy(
                    x = symbol.x - sourceXOffset + targetXOffset + offsetX,
                    y = symbol.y - sourceYOffset + targetYOffset + offsetY
                )
            }

            return copy(line = emptyGraph(), graph = target, width = width, height = height, symbols = movedSymbols)
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

    /**
     * A tree panel (rules/minecraft/01-1-tree-panel.md). [levels] is the size in the player's
     * unit, branch steps from the first fork to a tip; [width] and [height] are the square panel
     * the crown needs, in units, and are not a function of levels a reader should rely on.
     */
    data class Tree(
        override val line: Graph<Node>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
        val levels: Int,
        override val tutorial: Boolean = false,
        override val symbols: List<CellSymbol> = emptyList(),
    ) : Panel(Type.Tree) {

        companion object {
            /**
             * The Orchard's own maximum: 16 tips on one panel
             * (rules/minecraft/01-1-tree-panel.md#the-height-cap).
             */
            const val MAX_LEVELS: Int = 4

            /** Smallest panel a tree is drawn on, in units, so a one-level tree isn't a thumbnail. */
            const val MIN_SIZE: Int = 3

            /**
             * Proportions measured off the Orchard's first panel
             * (rules/minecraft/01-1-tree-panel.md#layout), as fractions of the panel: the crown's
             * width, the tree's height from root to tips, and the least distance between
             * neighbouring tips as a multiple of the line (`PuzzleSolver.LINE_THICKNESS`). Seen
             * in game 2026-09-05: with the grid's half-unit margin, tips two lines apart and
             * levels halving, the tree filled the panel edge to edge and read nothing like the
             * game's; these numbers are what closed the gap, so change them against a shot.
             */
            const val CROWN_WIDTH: Float = 0.77f
            const val TREE_HEIGHT: Float = 0.65f
            const val TIP_GAP_IN_LINES: Float = 1.4f
            private const val LINE: Float = 0.25f

            /**
             * Height of the trunk, then of each level from the first fork up, relative to each
             * other. Measured off the same panel: trunk 20, then 27 / 26 / 15 / 12 of the tree's
             * height. A shorter tree uses the first entries, so its crown is the same shape.
             */
            val LEVEL_WEIGHTS: List<Float> = listOf(0.75f, 1f, 1f, 0.6f, 0.45f)

            /** The square panel, in units, whose [CROWN_WIDTH] fits `2^levels` tips at [TIP_GAP_IN_LINES]. */
            fun sizeFor(levels: Int): Int {
                val tips: Int = 2.pow(levels)
                val crown: Float = (tips - 1) * TIP_GAP_IN_LINES * LINE
                return maxOf(MIN_SIZE, ceil(crown / CROWN_WIDTH).toInt())
            }

            /**
             * [levels] is the tree's size in the player's unit: branch steps from the first fork
             * to a tip. Comes with the Orchard's marks: a start on the root and an end on every
             * tip (rules/minecraft/01-1-tree-panel.md#what-a-tree-panel-is).
             */
            fun ofSize(levels: Int): Tree {
                val size: Int = sizeFor(levels)
                val blank: ValueGraph<Node, Edge> = generateTree(levels)
                val root: Node = blank.nodes().minBy(Node::y)
                val graph: MutableValueGraph<Node, Edge> = Graphs.copyOf(blank)
                    .withNodeReplaced(root, root.copy(modifier = Modifier.START))
                    .withTipEnds()
                return Tree(
                    line = emptyGraph(),
                    backgroundColor = DyeColor.WHITE,
                    graph = graph,
                    width = size,
                    height = size,
                    levels = levels
                )
            }

            /**
             * A full binary tree of [levels] branch steps on a [sizeFor] panel, blank
             * (rules/minecraft/01-1-tree-panel.md#layout): `2^levels` tips spread evenly across
             * a crown [CROWN_WIDTH] of the panel wide, the tree [TREE_HEIGHT] of the panel tall
             * and centred both ways, every parent centred under its pair of children, a trunk
             * below the first fork, and the root alone at the trunk's foot. Level heights follow
             * [LEVEL_WEIGHTS].
             */
            @Suppress("UnstableApiUsage")
            fun generateTree(levels: Int): ValueGraph<Node, Edge> {
                val graph: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
                val size: Int = sizeFor(levels)
                val tipCount: Int = 2.pow(levels)

                val crown: Float = size * CROWN_WIDTH
                val crownLeft: Float = (size - crown) / 2
                val treeHeight: Float = size * TREE_HEIGHT
                val rootY: Float = (size - treeHeight) / 2
                val crownY: Float = rootY + treeHeight

                // Heights bottom-up: the trunk, then one per level, scaled to the tree's height.
                val weights: List<Float> = LEVEL_WEIGHTS.take(levels + 1)
                val scale: Float = treeHeight / weights.sum()
                val levelHeights: List<Float> = weights.drop(1).map { weight -> weight * scale }

                var row: List<Node> = List(tipCount) { index ->
                    val x: Float = crownLeft + if (tipCount == 1) crown / 2 else index * crown / (tipCount - 1)
                    Node(x, crownY)
                }
                row.forEach { tip -> graph.addNode(tip) }
                var y: Float = crownY
                levelHeights.asReversed().forEach { levelHeight ->
                    y -= levelHeight
                    val parentY: Float = y
                    row = row.chunked(2).map { children ->
                        val parent = Node(children.map { it.x }.sum() / children.size, parentY)
                        graph.addNode(parent)
                        children.forEach { child -> graph.putEdgeValue(parent, child, Edge.NORMAL) }
                        parent
                    }
                }
                val fork: Node = row.single()
                val root = Node(fork.x, rootY)
                graph.putEdgeValue(root, fork, Edge.NORMAL)
                return graph
            }

            /** The tips: every node with one branch that is not itself a nub, and not the root. */
            @Suppress("UnstableApiUsage")
            internal fun ValueGraph<Node, Edge>.tips(): List<Node> {
                val root: Node = nodes().filter { it.modifier != Modifier.END }.minBy(Node::y)
                return nodes()
                    .filter { node -> node.modifier != Modifier.END && node != root }
                    .filter { node -> adjacentNodes(node).none { it.y > node.y && it.modifier != Modifier.END } }
                    .sortedBy(Node::x)
            }

            /**
             * An end nub straight up off every tip that has none, the Orchard's default, skipping
             * the tips in [except]: growth passes the author's own nodes there, so a stub they
             * left bare stays bare.
             */
            @Suppress("UnstableApiUsage")
            internal fun MutableValueGraph<Node, Edge>.withTipEnds(except: Set<Node> = emptySet()): MutableValueGraph<Node, Edge> = apply {
                tips().forEach { tip ->
                    if (tip in except) return@forEach
                    if (adjacentNodes(tip).any { it.modifier == Modifier.END }) return@forEach
                    putEdgeValue(tip, Node(tip.x, tip.y + END_POINT_LENGTH, Modifier.END), Edge.NORMAL)
                }
            }

            /**
             * [node] and everything that hangs above it, nubs included: what the eraser takes when
             * it prunes a limb (rules/minecraft/01-1-tree-panel.md#pruning-the-grid-tab-on-a-tree).
             */
            @Suppress("UnstableApiUsage")
            internal fun ValueGraph<Node, Edge>.subtreeOf(node: Node): Set<Node> {
                val limb: MutableSet<Node> = mutableSetOf(node)
                val pending: ArrayDeque<Node> = ArrayDeque(listOf(node))
                while (pending.isNotEmpty()) {
                    val current: Node = pending.removeFirst()
                    adjacentNodes(current)
                        .filter { it !in limb && (it.y > current.y || it.modifier == Modifier.END) }
                        .forEach { above -> limb += above; pending += above }
                }
                return limb
            }

            /** This graph with [old] swapped for [new], every edge kept. */
            @Suppress("UnstableApiUsage")
            internal fun MutableValueGraph<Node, Edge>.withNodeReplaced(old: Node, new: Node): MutableValueGraph<Node, Edge> = apply {
                if (old == new) return@apply
                val neighbours: List<Pair<Node, Edge>> = adjacentNodes(old).map { neighbour ->
                    neighbour to (edgeValueOf(old, neighbour) ?: Edge.NORMAL)
                }
                removeNode(old)
                addNode(new)
                neighbours.forEach { (neighbour, edge) -> putEdgeValue(new, neighbour, edge) }
            }

            /**
             * The node a walk by branch position starts from: the first fork. On a tree laid out
             * before the trunk existed the root *is* the first fork, so a tree with a two-child
             * root is read that way rather than mistaking its left branch for a trunk.
             */
            @Suppress("UnstableApiUsage")
            internal fun firstFork(graph: ValueGraph<Node, Edge>, root: Node): Node {
                val branches: List<Node> = graph.adjacentNodes(root).filter { it.y > root.y && it.modifier != Modifier.END }
                return branches.singleOrNull() ?: root
            }
        }

        /**
         * A copy [levels] tall with this tree's marks transplanted into it by **branch position**
         * (rules/minecraft/01-1-tree-panel.md#growing-a-tree). Node coordinates are recomputed on
         * every size change, so nothing is matched by position: both trees are walked from the
         * root in parallel, children ordered left to right, and each source node's role and
         * symbol land on the target node at the same turn sequence. The source becomes the bottom
         * levels of the result.
         *
         * Pruning survives: a limb the source does not have stays absent in the result, and a
         * source child is matched to the target child on the same side of its parent rather than
         * by order, so a lone surviving branch lands where it was.
         *
         * End nubs: a matched node that is still a tip keeps its nub (a pruned stub with an end
         * stays a short branch, a bare stub stays the broken branch), the root keeps its nub, and
         * every tip the template adds gets a fresh end, like a fresh tree's. The old tips that
         * became forks lose theirs. The drawn [line] is dropped, as on a grid.
         */
        @Suppress("UnstableApiUsage")
        fun expandTo(levels: Int): Tree {
            if (levels <= this.levels) return this

            val target: ValueGraph<Node, Edge> = generateTree(levels)
            val transplanted: MutableValueGraph<Node, Edge> = ValueGraphBuilder.undirected().build()
            val replacements: MutableMap<Node, Node> = mutableMapOf()
            val carriedEdges: MutableMap<Set<Node>, Edge> = mutableMapOf()
            /** Target node -> the source node it stands for. */
            val matched: MutableMap<Node, Node> = mutableMapOf()
            val pruned: MutableSet<Node> = mutableSetOf()

            fun ValueGraph<Node, Edge>.children(node: Node): List<Node> =
                adjacentNodes(node).filter { it.y > node.y && it.modifier != Modifier.END }.sortedBy(Node::x)

            fun walk(source: Node, into: Node, depth: Int) {
                replacements[into] = into.copy(modifier = source.modifier, symbol = source.symbol)
                matched[into] = source
                // At the source crown every target child is new growth; below it, a side with
                // no source child is a pruned limb, and it stays that way.
                if (depth == this.levels) return
                val sourceChildren: List<Node> = graph.children(source)
                target.children(into).forEach { targetChild ->
                    val left: Boolean = targetChild.x < into.x
                    val sourceChild: Node? = sourceChildren.firstOrNull { (it.x < source.x) == left }
                    if (sourceChild == null) {
                        pruned += targetChild
                        return@forEach
                    }
                    graph.edgeValue(source, sourceChild).orElse(null)?.let { edge ->
                        carriedEdges[setOf(into, targetChild)] = edge
                    }
                    walk(sourceChild, targetChild, depth + 1)
                }
            }

            val sourceRoot: Node = graph.nodes().filter { it.modifier != Modifier.END }.minBy(Node::y)
            val targetRoot: Node = target.nodes().minBy(Node::y)
            // The root and the first fork are matched by role, not by walking, so a tree from
            // before the trunk existed (root doubling as the fork) still lands on the fork.
            replacements[targetRoot] = targetRoot.copy(modifier = sourceRoot.modifier, symbol = sourceRoot.symbol)
            matched[targetRoot] = sourceRoot
            val sourceFork: Node = firstFork(graph, sourceRoot)
            val targetFork: Node = firstFork(target, targetRoot)
            if (sourceFork != sourceRoot) {
                graph.edgeValue(sourceRoot, sourceFork).orElse(null)?.let { edge ->
                    carriedEdges[setOf(targetRoot, targetFork)] = edge
                }
            }
            walk(sourceFork, targetFork, 0)
            if (sourceFork == sourceRoot) {
                // Old layout: the root's marks belong to the fork it was; the new foot stays blank.
                replacements[targetRoot] = targetRoot
                matched.remove(targetRoot)
            }

            // Everything above a pruned limb goes with it, so a source tip that was a stub stays one.
            val absent: Set<Node> = pruned.flatMap { limb -> target.subtreeOf(limb) }.toSet()

            target.nodes().filter { it !in absent }.forEach { node -> transplanted.addNode(replacements[node] ?: node) }
            target.edges().forEach { pair ->
                if (pair.nodeU() in absent || pair.nodeV() in absent) return@forEach
                val u: Node = replacements[pair.nodeU()] ?: pair.nodeU()
                val v: Node = replacements[pair.nodeV()] ?: pair.nodeV()
                val edge: Edge = carriedEdges[setOf(pair.nodeU(), pair.nodeV())] ?: (target.edgeValueOf(pair) ?: Edge.NORMAL)
                transplanted.putEdgeValue(u, v, edge)
            }

            // Nubs travel with the nodes that keep the right to one: the root, and any matched
            // node that is still a tip. Same offset from its node, same edge.
            matched.forEach { (targetNode, sourceNode) ->
                val stillTip: Boolean = targetNode == targetRoot || transplanted.children(replacements.getValue(targetNode)).isEmpty()
                if (!stillTip) return@forEach
                val placed: Node = replacements.getValue(targetNode)
                graph.adjacentNodes(sourceNode).filter { it.modifier == Modifier.END }.forEach { nub ->
                    val moved: Node = nub.copy(x = placed.x + (nub.x - sourceNode.x), y = placed.y + (nub.y - sourceNode.y))
                    transplanted.putEdgeValue(placed, moved, graph.edgeValue(sourceNode, nub).orElse(Edge.NORMAL))
                }
            }

            // Tips the template added get the Orchard's ends, like a fresh tree's. A matched tip
            // is the author's, with or without one.
            val authored: Set<Node> = matched.keys.map { replacements.getValue(it) }.toSet()
            transplanted.withTipEnds(except = authored)

            val size: Int = sizeFor(levels)
            return copy(line = emptyGraph(), graph = transplanted, width = size, height = size, levels = levels)
        }

        override fun resize(length: Int): Tree = expandTo(length)
    }

    data class Freeform(
        override val line: Graph<Node>,
        override val graph: ValueGraph<Node, Edge>,
        override val backgroundColor: DyeColor,
        override val width: Int,
        override val height: Int,
        override val tutorial: Boolean = false,
        override val symbols: List<CellSymbol> = emptyList(),
    ) : Panel(Type.Freeform) {
        override fun resize(length: Int): Freeform = TODO()
    }

    companion object {
        /**
         * Codec used by the `witness:panel` data component. It reuses the existing NBT
         * (de)serialization so the stored shape matches the pre-1.20.5 raw-NBT `panel` tag,
         * and recipe JSON can specify the component with the same structure.
         */
        val CODEC: Codec<Panel> = CompoundTag.CODEC.xmap(
            { nbt -> nbt.toPanel() },
            { panel -> panel.toNbt() }
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Panel> = ByteBufCodecs.fromCodecWithRegistries(CODEC)

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
fun CompoundTag.toPanel(): Panel {
    val type: Type = Type.values()[getIntTolerant(KEY_PANEL_TYPE)]
    val line: Graph<Node> = getGraph(KEY_LINE)

    val backgroundColor: DyeColor = DyeColor.values()[getIntTolerant(KEY_BACKGROUND_COLOR)]
    val grid: ValueGraph<Node, Edge> = getValueGraph(KEY_GRAPH)
    // Empty on pre-flag panels; those are not tutorials.
    val tutorial: Boolean = getBooleanTolerant(KEY_TUTORIAL)
    // Absent on every panel saved before region symbols existed.
    val symbols: List<CellSymbol> = getCellSymbols(KEY_SYMBOLS)

    return when (type) {
        Type.Grid -> Panel.Grid(line, grid, backgroundColor, getIntTolerant(KEY_WIDTH), getIntTolerant(KEY_HEIGHT), tutorial, symbols)
        // Trees saved before `levels` was its own field were `levels + 1` units tall.
        Type.Tree -> Panel.Tree(
            line, grid, backgroundColor,
            getIntTolerant(KEY_HEIGHT), getIntTolerant(KEY_HEIGHT),
            getIntTolerant(KEY_LEVELS, getIntTolerant(KEY_HEIGHT) - 1),
            tutorial, symbols
        )
        Type.Freeform -> Panel.Freeform(line, grid, backgroundColor, getIntTolerant(KEY_WIDTH), getIntTolerant(KEY_HEIGHT), tutorial, symbols)
    }
}

/** Writes this panel's fields into a fresh compound (inverse of [toPanel]). */
fun Panel.toNbt(): CompoundTag = CompoundTag().also { tag ->
    tag.putInt(KEY_PANEL_TYPE, type.ordinal)
    tag.putGraph(KEY_LINE, line)
    tag.putInt(KEY_BACKGROUND_COLOR, backgroundColor.ordinal)
    tag.putValueGraph(KEY_GRAPH, graph)
    tag.putBoolean(KEY_TUTORIAL, tutorial)
    tag.putCellSymbols(KEY_SYMBOLS, symbols)
    when (this) {
        is Panel.Grid, is Panel.Freeform -> {
            tag.putInt(KEY_WIDTH, width)
            tag.putInt(KEY_HEIGHT, height)
        }

        is Panel.Tree -> {
            tag.putInt(KEY_HEIGHT, height)
            tag.putInt(KEY_LEVELS, levels)
        }
    }
}

/** Copy with [tutorial] set; preserves every other field. */
fun Panel.withTutorial(tutorial: Boolean): Panel = when (this) {
    is Panel.Grid -> copy(tutorial = tutorial)
    is Panel.Tree -> copy(tutorial = tutorial)
    is Panel.Freeform -> copy(tutorial = tutorial)
}

/** Copy with [symbols] replaced; preserves every other field. */
fun Panel.withSymbols(symbols: List<CellSymbol>): Panel = when (this) {
    is Panel.Grid -> copy(symbols = symbols)
    is Panel.Tree -> copy(symbols = symbols)
    is Panel.Freeform -> copy(symbols = symbols)
}

@Suppress("UnstableApiUsage")
fun CompoundTag.getPanel(key: String): Panel? {
    if (!contains(key)) return null
    return getCompoundOrEmpty(key).toPanel()
}

fun CompoundTag.putPanel(key: String, panel: Panel) {
    put(key, panel.toNbt())
}