package com.xfastgames.witness.items.data

import com.google.common.truth.Truth.assertThat
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.DyeColor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Colored squares (rules/witness/06-colored-squares.md): the region partition the finished line
 * makes, and the one-colour-per-region rule checked against it.
 *
 * The panel is a fresh 3x3-node grid, so its cells are at (1,1), (2,1), (1,2), (2,2):
 *
 * ```
 *   (0.5,2.5) --- (1.5,2.5) --- (2.5,2.5)
 *       |    (1,2)    |    (2,2)    |
 *   (0.5,1.5) --- (1.5,1.5) --- (2.5,1.5)
 *       |    (1,1)    |    (2,1)    |
 *   (0.5,0.5) --- (1.5,0.5) --- (2.5,0.5)
 * ```
 */
@Suppress("UnstableApiUsage")
class SquareTests {

    private val grid: Panel.Grid = Panel.Grid.ofSize(3)

    private fun Panel.at(x: Float, y: Float): Node = graph.nodes().single { it.x == x && it.y == y }

    /** Bottom-left to top-right along the border: encloses nothing, one region. */
    private fun Panel.aroundTheOutside(): List<Node> =
        listOf(at(0.5f, 0.5f), at(1.5f, 0.5f), at(2.5f, 0.5f), at(2.5f, 1.5f), at(2.5f, 2.5f))

    /** Bottom-middle straight up the middle column: left cells from right cells. */
    private fun Panel.upTheMiddle(): List<Node> =
        listOf(at(1.5f, 0.5f), at(1.5f, 1.5f), at(1.5f, 2.5f))

    private fun square(x: Float, y: Float, color: DyeColor) = CellSymbol(x, y, Figure.SQUARE, color)

    @Nested
    @DisplayName("Regions")
    inner class Regions {

        @Test
        fun `a 3x3 grid has four cells`() {
            assertThat(grid.cells()).containsExactly(Node(1f, 1f), Node(2f, 1f), Node(1f, 2f), Node(2f, 2f))
        }

        @Test
        fun `a tree has no cells`() {
            assertThat(Panel.Tree.ofSize(2).cells()).isEmpty()
        }

        @Test
        fun `a line along the border leaves one region`() {
            assertThat(grid.regions(grid.aroundTheOutside())).hasSize(1)
        }

        @Test
        fun `a line up the middle splits left from right`() {
            val regions: List<Set<Node>> = grid.regions(grid.upTheMiddle())

            assertThat(regions).containsExactly(
                setOf(Node(1f, 1f), Node(1f, 2f)),
                setOf(Node(2f, 1f), Node(2f, 2f))
            )
        }

        @Test
        fun `an empty path leaves one region`() {
            assertThat(grid.regions(emptyList())).hasSize(1)
        }

        @Test
        fun `a broken edge does not cut a region`() {
            // The edge between the two bottom cells is broken, but nothing was drawn on it.
            val broken: Panel = grid.withGraph(
                com.google.common.graph.Graphs.copyOf(grid.graph).apply {
                    putEdgeValue(grid.at(1.5f, 0.5f), grid.at(1.5f, 1.5f), Edge.BREAK)
                }
            )

            assertThat(broken.regions(emptyList())).hasSize(1)
        }

        @Test
        fun `a carved-away segment does not cut a region`() {
            val carved: Panel = requireNotNull(grid.withSegmentRemoved(1.5f, 0.5f, 1.5f, 1.5f))

            assertThat(carved.regions(emptyList())).hasSize(1)
        }

        @Test
        fun `the path is matched by position, not by node identity`() {
            // A start node is a different Node from the bare position, and the solver's path holds
            // the graph's marked copies.
            val marked: Panel = grid.withNodeReplaced(grid.at(1.5f, 0.5f), Node(1.5f, 0.5f, Modifier.START))

            assertThat(marked.regions(marked.upTheMiddle())).hasSize(2)
        }
    }

    @Nested
    @DisplayName("The square rule")
    inner class Rule {

        @Test
        fun `no squares passes`() {
            assertThat(grid.clashingSquares(grid.aroundTheOutside())).isEmpty()
        }

        @Test
        fun `same colours in one region pass`() {
            val panel: Panel = grid.withSymbols(listOf(square(1f, 1f, DyeColor.BLACK), square(2f, 2f, DyeColor.BLACK)))

            assertThat(panel.clashingSquares(panel.aroundTheOutside())).isEmpty()
        }

        @Test
        fun `two colours in one region fail, and every square in it is at fault`() {
            val black = square(1f, 1f, DyeColor.BLACK)
            val white = square(2f, 2f, DyeColor.WHITE)
            val panel: Panel = grid.withSymbols(listOf(black, white))

            assertThat(panel.clashingSquares(panel.aroundTheOutside())).containsExactly(black, white)
        }

        @Test
        fun `the line separating the colours passes`() {
            val panel: Panel = grid.withSymbols(listOf(square(1f, 1f, DyeColor.BLACK), square(2f, 2f, DyeColor.WHITE)))

            assertThat(panel.clashingSquares(panel.upTheMiddle())).isEmpty()
        }

        @Test
        fun `only the mixed region's squares are reported`() {
            val blackLeft = square(1f, 1f, DyeColor.BLACK)
            val whiteLeft = square(1f, 2f, DyeColor.WHITE)
            val blackRight = square(2f, 1f, DyeColor.BLACK)
            val panel: Panel = grid.withSymbols(listOf(blackLeft, whiteLeft, blackRight))

            assertThat(panel.clashingSquares(panel.upTheMiddle())).containsExactly(blackLeft, whiteLeft)
        }
    }

    @Nested
    @DisplayName("Authoring")
    inner class Authoring {

        @Test
        fun `clicking an empty cell places a black square`() {
            val panel: Panel = requireNotNull(grid.withSquareCycled(1.2f, 0.8f))

            assertThat(panel.symbols).containsExactly(square(1f, 1f, DyeColor.BLACK))
        }

        @Test
        fun `clicking cycles black to white to gone`() {
            val black: Panel = requireNotNull(grid.withSquareCycled(1f, 1f))
            val white: Panel = requireNotNull(black.withSquareCycled(1f, 1f))
            val gone: Panel = requireNotNull(white.withSquareCycled(1f, 1f))

            assertThat(white.symbols.single().color).isEqualTo(DyeColor.WHITE)
            assertThat(gone.symbols).isEmpty()
        }

        @Test
        fun `clicking outside every cell is refused`() {
            assertThat(grid.withSquareCycled(0.2f, 0.2f)).isNull()
            assertThat(Panel.Tree.ofSize(2).withSquareCycled(1f, 1f)).isNull()
        }

        @Test
        fun `carving on the grid tab keeps the square`() {
            val panel: Panel = requireNotNull(grid.withSquareCycled(1f, 1f))

            val carved: Panel = panel.withNodeRemoved(panel.at(0.5f, 0.5f))

            assertThat(carved.symbols).isEqualTo(panel.symbols)
        }
    }

    @Nested
    @DisplayName("Persistence")
    inner class Persistence {

        @Test
        fun `squares survive an NBT round trip`() {
            val panel: Panel = grid.withSymbols(listOf(square(1f, 1f, DyeColor.BLACK), square(2f, 2f, DyeColor.WHITE)))

            assertThat(panel.toNbt().toPanel()).isEqualTo(panel)
        }

        @Test
        fun `a panel saved before squares reads back with none`() {
            val tag: CompoundTag = grid.toNbt().apply { remove("symbols") }

            assertThat(tag.toPanel().symbols).isEmpty()
        }

        @Test
        fun `growing a grid carries a square across through the offsets`() {
            // 2x2 nodes -> one cell at (1,1). Grown to 2 wide x 4 tall, the lattice recentres on x
            // (offset 1.5), so the same cell now sits at (2,1); its raw coordinate would be wrong.
            val small: Panel.Grid = Panel.Grid.ofSize(2).withSymbols(listOf(square(1f, 1f, DyeColor.BLACK))) as Panel.Grid

            val grown: Panel.Grid = small.expandTo(2, 4)

            assertThat(grown.symbols.single()).isEqualTo(square(2f, 1f, DyeColor.BLACK))
            assertThat(grown.cells()).contains(Node(2f, 1f))
        }
    }
}
