package com.xfastgames.witness.blocks.redstone

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * A flat floor at y = 0 with air above it: every cell at y = 1 is supported by the floor, and the
 * floor itself is solid. Walls are extra solid cells.
 */
class CablePathTests {

    private fun world(walls: Set<Cell> = emptySet(), cables: Set<Cell> = emptySet()): Pather {
        val solid: (Cell) -> Boolean = { cell -> cell.y <= 0 || cell in walls }
        return Pather(
            passable = { cell -> !solid(cell) },
            supported = { cell -> Cell.TOUCHING.any { touch -> solid(cell + touch) } },
            existing = { cell -> cell in cables },
        )
    }

    private class Pather(
        val passable: (Cell) -> Boolean,
        val supported: (Cell) -> Boolean,
        val existing: (Cell) -> Boolean,
    ) {
        fun path(start: Cell, end: Cell, maxLength: Int = CABLE_MAX_DISTANCE): List<Cell>? =
            findCablePath(start, end, passable, supported, existing, maxLength)
    }

    @Test
    fun `A straight run along the floor is the straight line`() {
        val path = world().path(Cell(0, 1, 0), Cell(5, 1, 0))

        assertThat(path).containsExactly(
            Cell(0, 1, 0), Cell(1, 1, 0), Cell(2, 1, 0), Cell(3, 1, 0), Cell(4, 1, 0), Cell(5, 1, 0),
        ).inOrder()
    }

    @Test
    fun `A run round a corner turns once`() {
        val path = world().path(Cell(0, 1, 0), Cell(3, 1, 3))!!

        assertThat(path).hasSize(7)
        assertThat(turns(path)).isEqualTo(1)
    }

    @Test
    fun `A run stays on the floor rather than floating`() {
        val path = world().path(Cell(0, 1, 0), Cell(4, 1, 0))!!

        assertThat(path.all { it.y == 1 }).isTrue()
    }

    @Test
    fun `A run climbs a wall it cannot pass`() {
        // A wall two high across x = 2, the whole width of the world.
        val wall: Set<Cell> = (-8..8).flatMap { z -> listOf(Cell(2, 1, z), Cell(2, 2, z)) }.toSet()
        val path = world(walls = wall).path(Cell(0, 1, 0), Cell(4, 1, 0))!!

        assertThat(path.none { it in wall }).isTrue()
        assertThat(path.maxOf { it.y }).isEqualTo(3)
        // Every cell touches something: the floor, the wall, or the corner of the wall's top.
        assertThat(path.all { cell -> Cell.TOUCHING.any { touch -> (cell + touch).let { it.y <= 0 || it in wall } } }).isTrue()
    }

    @Test
    fun `A run prefers cables already laid`() {
        // An existing run along z = 2 from x = 0 to x = 5; the direct line along z = 0 is one turn shorter.
        val laid: Set<Cell> = (0..5).map { x -> Cell(x, 1, 2) }.toSet()
        val path = world(cables = laid).path(Cell(0, 1, 0), Cell(5, 1, 0))!!

        assertThat(path.count { it in laid }).isEqualTo(6)
    }

    @Test
    fun `A walled-off target has no route`() {
        val box: Set<Cell> = buildSet {
            for (x in 4..6) for (z in -1..1) for (y in 1..3) if (x != 5 || z != 0 || y == 3) add(Cell(x, y, z))
        }
        assertThat(world(walls = box).path(Cell(0, 1, 0), Cell(5, 1, 0))).isNull()
    }

    @Test
    fun `A target past the maximum length has no route`() {
        assertThat(world().path(Cell(0, 1, 0), Cell(10, 1, 0), maxLength = 10)).isNull()
        assertThat(world().path(Cell(0, 1, 0), Cell(9, 1, 0), maxLength = 10)).isNotNull()
    }

    @Test
    fun `Start and end are the whole route when they touch`() {
        assertThat(world().path(Cell(0, 1, 0), Cell(0, 1, 0))).containsExactly(Cell(0, 1, 0))
        assertThat(world().path(Cell(0, 1, 0), Cell(1, 1, 0))).containsExactly(Cell(0, 1, 0), Cell(1, 1, 0)).inOrder()
    }

    private fun turns(path: List<Cell>): Int =
        path.zipWithNext { a, b -> Cell(b.x - a.x, b.y - a.y, b.z - a.z) }.zipWithNext().count { (m1, m2) -> m1 != m2 }
}
