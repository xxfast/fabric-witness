package com.xfastgames.witness.blocks.redstone

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** A block position without Minecraft. */
data class Cell(val x: Int, val y: Int, val z: Int) {
    operator fun plus(other: Cell): Cell = Cell(x + other.x, y + other.y, z + other.z)
}

/** Runs as sets of cells; arms are whichever neighbours are also in the run. */
class CableRibbonTests {

    private fun step(way: Way): Cell = when (way) {
        Way.DOWN -> Cell(0, -1, 0); Way.UP -> Cell(0, 1, 0)
        Way.NORTH -> Cell(0, 0, -1); Way.SOUTH -> Cell(0, 0, 1)
        Way.WEST -> Cell(-1, 0, 0); Way.EAST -> Cell(1, 0, 0)
    }

    private fun widths(run: List<Cell>, floorY: Int = 0, seeds: Map<Cell, Axis> = emptyMap()): Map<Cell, Axis> =
        ribbonWidths(
            cells = run,
            arms = { cell -> Way.entries.filter { way -> cell + step(way) in run }.toSet() },
            floor = { cell -> cell.y == floorY },
            neighbour = { cell, way -> cell + step(way) },
            seeds = seeds,
        )

    @Test
    fun `A floor run lies flat`() {
        val run = listOf(Cell(0, 0, 0), Cell(0, 0, 1), Cell(0, 0, 2))
        assertThat(widths(run).values.toSet()).containsExactly(Axis.X)
    }

    @Test
    fun `A climb out of the floor is wide across the floor arm, and a band that turns with it stands`() {
        // Floor run heading north (along z), up two, then a band heading east (along x).
        val run = listOf(Cell(0, 0, 2), Cell(0, 0, 1), Cell(0, 0, 0), Cell(0, 1, 0), Cell(0, 2, 0), Cell(1, 2, 0))
        val w = widths(run)

        assertThat(w[Cell(0, 0, 0)]).isEqualTo(Axis.X)   // the foot: wide across z
        assertThat(w[Cell(0, 1, 0)]).isEqualTo(Axis.X)   // the rod keeps it
        assertThat(w[Cell(0, 2, 0)]).isEqualTo(Axis.Y)   // width x meets a band along x: round the edge, standing
        assertThat(w[Cell(1, 2, 0)]).isEqualTo(Axis.Y)
    }

    @Test
    fun `A band that continues the floor's direction lies flat over its face`() {
        // Floor run heading north, up two, then a band heading north again.
        val run = listOf(Cell(0, 0, 2), Cell(0, 0, 1), Cell(0, 0, 0), Cell(0, 1, 0), Cell(0, 2, 0), Cell(0, 2, -1))
        val w = widths(run)

        assertThat(w[Cell(0, 2, 0)]).isEqualTo(Axis.X)
        assertThat(w[Cell(0, 2, -1)]).isEqualTo(Axis.X)
    }

    @Test
    fun `The floor decides before a frame does`() {
        // Floor run heading north, up two, band heading north into a frame: the floor makes the climb wide
        // across x, so the band lies flat over its face into the panel's side; the frame's wish to stand is too late.
        val run = listOf(Cell(0, 0, 2), Cell(0, 0, 1), Cell(0, 0, 0), Cell(0, 1, 0), Cell(0, 2, 0), Cell(0, 2, -1))
        val w = widths(run, seeds = mapOf(Cell(0, 2, -1) to Axis.Y))

        assertThat(w[Cell(0, 0, 0)]).isEqualTo(Axis.X)
        assertThat(w[Cell(0, 1, 0)]).isEqualTo(Axis.X)
        assertThat(w[Cell(0, 2, 0)]).isEqualTo(Axis.X)
        assertThat(w[Cell(0, 2, -1)]).isEqualTo(Axis.X)
    }

    @Test
    fun `A two-high climb along the panel's plane enters the panel flat, and no cell twists`() {
        // F3 2026-08-30 19:02: floor arm west, up, then the band east into the frame directly above the foot.
        val run = listOf(Cell(-1, 0, 0), Cell(0, 0, 0), Cell(0, 1, 0), Cell(1, 1, 0))
        val w = widths(run, seeds = mapOf(Cell(1, 1, 0) to Axis.Y))

        assertThat(w[Cell(0, 0, 0)]).isEqualTo(Axis.Z)
        assertThat(w[Cell(0, 1, 0)]).isEqualTo(Axis.Z)
        assertThat(w[Cell(1, 1, 0)]).isEqualTo(Axis.Z)
    }

    @Test
    fun `A climb across the panel's plane stands into its side on its own`() {
        // Floor run heading north, up two, band heading east into a frame: the foot is wide across x and a
        // band along x turns that round its edge, so it stands without the frame having to say so.
        val run = listOf(Cell(0, 0, 2), Cell(0, 0, 1), Cell(0, 0, 0), Cell(0, 1, 0), Cell(0, 2, 0), Cell(1, 2, 0))
        assertThat(widths(run)[Cell(1, 2, 0)]).isEqualTo(Axis.Y)
        assertThat(widths(run, seeds = mapOf(Cell(1, 2, 0) to Axis.Y))[Cell(1, 2, 0)]).isEqualTo(Axis.Y)
    }

    @Test
    fun `A suspended run with nothing to decide it lies flat`() {
        val run = listOf(Cell(0, 3, 0), Cell(1, 3, 0), Cell(1, 3, 1))
        assertThat(widths(run).values.toSet()).doesNotContain(Axis.Y)
    }

    @Test
    fun `Standing stays standing round a corner and down a drop`() {
        val run = listOf(Cell(0, 3, 0), Cell(1, 3, 0), Cell(1, 3, 1), Cell(1, 2, 1), Cell(1, 1, 1))
        val w = widths(run, floorY = -1, seeds = mapOf(Cell(0, 3, 0) to Axis.Y))

        assertThat(w[Cell(1, 3, 0)]).isEqualTo(Axis.Y)
        assertThat(w[Cell(1, 3, 1)]).isEqualTo(Axis.Y)
        assertThat(w[Cell(1, 2, 1)]).isEqualTo(Axis.Z)   // the standing band along z turns down round its edge: wide across z
        assertThat(w[Cell(1, 1, 1)]).isEqualTo(Axis.Z)
    }
}
