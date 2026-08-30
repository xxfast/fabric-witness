package com.xfastgames.witness.blocks.redstone

/**
 * Which way the ribbon is wide in each cable of a run
 * (rules/minecraft/06-cable.md, "Bends"). Pure over an abstract cell so it runs in unit tests;
 * the block supplies the arms and seeds.
 *
 * A flat ribbon can bend two ways: over its face (the width axis is the bend axis, so it stays)
 * or round its edge (the width axis turns with the bend). Which one a joint uses follows from
 * the ribbon's width axis on the way in, so one axis per cell, carried along the run, decides
 * every joint. Horizontal pieces store [Axis.Y] when they stand on edge and a horizontal axis
 * when they lie flat; a vertical piece stores the horizontal axis it is wide across; a floor
 * piece with a climb stores its rod's width, which its own arm fixes (flat, rising face-first).
 *
 * Every cable on the floor decides itself (flat, rising face-first) and spreads to the end;
 * then the seeds decide whatever the floor did not reach (a band out of a frame stands in the
 * panel's plane; a foot under a stand faces the stand) and spread; the rest lie flat. The floor
 * goes first because a flat floor arm can only rise face-first, and a frame that disagreed with
 * it would need a quarter twist somewhere, which JSON models can only draw as a stack of 22.5°
 * slabs (tried 2026-08-30 19:22, jarring). So a run arriving along a panel's plane enters its
 * side flat; one arriving across it stands, which the floor produces on its own.
 *
 * Ties go to whichever decider is seeded first, so [cells] and [seeds] must come in an order
 * that does not depend on who asked: the caller sorts them.
 */
fun <T> ribbonWidths(
    cells: Collection<T>,
    arms: (T) -> Set<Way>,
    floor: (T) -> Boolean,
    neighbour: (T, Way) -> T,
    seeds: Map<T, Axis>,
): Map<T, Axis> {
    val width: MutableMap<T, Axis> = linkedMapOf()
    val queue: ArrayDeque<T> = ArrayDeque()
    fun decide(cell: T, axis: Axis) {
        if (cell !in cells || cell in width) return
        // A floor piece with a horizontal arm lies flat and rises face-first, whatever arrives.
        width[cell] = if (floor(cell) && arms(cell).any { it.horizontal }) flatWidth(arms(cell)) else axis
        queue.addLast(cell)
    }
    fun spread() {
        while (queue.isNotEmpty()) {
            val cell: T = queue.removeFirst()
            val here: Axis = width.getValue(cell)
            arms(cell).forEach { way ->
                val next: T = neighbour(cell, way)
                if (next !in cells || next in width) return@forEach
                decide(next, if (way.horizontal) alongBand(here, way) else upOrDown(here, arms(cell), arms(next), floor(next)))
            }
        }
    }
    cells.filter { cell -> floor(cell) && arms(cell).any { it.horizontal } }.forEach { cell -> decide(cell, flatWidth(arms(cell))) }
    spread()
    seeds.forEach { (cell, axis) -> decide(cell, axis) }
    spread()
    cells.forEach { cell -> width.putIfAbsent(cell, flatWidth(arms(cell))) }
    return width
}

/** A flat horizontal piece is wide across its arm; any horizontal axis reads as flat, so the first arm's is fine. */
private fun flatWidth(arms: Set<Way>): Axis =
    arms.firstOrNull { it.horizontal }?.axis?.other ?: Axis.X

/** Along a band, standing stays standing and flat stays flat (wide across the way travelled). */
private fun alongBand(here: Axis, way: Way): Axis = if (here == Axis.Y) Axis.Y else way.axis.other

/**
 * Up or down out of [from] into [into]. The rod between them is wide across [from]'s band axis
 * if that band stands (an edge bend turns the width up into the rod), or keeps [from]'s width if
 * it lies flat (a face bend). Arriving at bands off the floor, the rod's width against the band's
 * axis decides again: equal means the ribbon turns round its edge and stands, otherwise it lies
 * over its face. A floor piece just takes the rod's width (and then overrides it, see [ribbonWidths]).
 */
private fun upOrDown(here: Axis, from: Set<Way>, into: Set<Way>, intoFloor: Boolean): Axis {
    val rod: Axis = rodWidth(here, from)
    if (intoFloor) return rod
    val band: Axis = into.firstOrNull { it.horizontal }?.axis ?: return rod
    return if (rod == band) Axis.Y else rod
}

/** The width of a rod leaving a piece of width [here] with [arms]: a standing band's turns round its edge into the band's axis. */
private fun rodWidth(here: Axis, arms: Set<Way>): Axis =
    if (here == Axis.Y) arms.firstOrNull { it.horizontal }?.axis ?: Axis.X else here

enum class Axis {
    X, Y, Z;

    /** The other horizontal axis; Y has none and answers X. */
    val other: Axis get() = when (this) { X -> Z; Z -> X; Y -> X }
}

/** The six arms, in the same order as Minecraft's `Direction` so ordinals line up. */
enum class Way(val axis: Axis) {
    DOWN(Axis.Y), UP(Axis.Y), NORTH(Axis.Z), SOUTH(Axis.Z), WEST(Axis.X), EAST(Axis.X);

    val horizontal: Boolean get() = axis != Axis.Y
}
