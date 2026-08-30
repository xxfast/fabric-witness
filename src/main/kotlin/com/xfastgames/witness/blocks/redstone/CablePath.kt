package com.xfastgames.witness.blocks.redstone

import java.util.PriorityQueue

/**
 * A block position without Minecraft, so the pather runs in unit tests. Steps are the six axis
 * moves; a turn is a step whose move differs from the one before.
 */
data class Cell(val x: Int, val y: Int, val z: Int) {
    operator fun plus(other: Cell): Cell = Cell(x + other.x, y + other.y, z + other.z)

    companion object {
        val MOVES: List<Cell> = listOf(
            Cell(0, -1, 0), Cell(0, 1, 0), Cell(0, 0, -1), Cell(0, 0, 1), Cell(-1, 0, 0), Cell(1, 0, 0),
        )

        /**
         * The 26 cells around one, edges and corners included. A cable is supported when any of
         * them is solid: "rests against" alone (the six faces) could never top a wall, since the
         * cell beside a wall's top face touches nothing, so a laid run could only go round.
         */
        val TOUCHING: List<Cell> = (-1..1).flatMap { x -> (-1..1).flatMap { y -> (-1..1).map { z -> Cell(x, y, z) } } }
            .filter { it != Cell(0, 0, 0) }
    }
}

/**
 * The route a laid run takes from [start] to [end] (rules/minecraft/06-cable.md, "Laying a run").
 * Pure over abstract predicates so it runs without Minecraft; the item supplies them.
 *
 * Dijkstra over (cell, arriving move): a new cable costs [NEW_COST], a cell that already holds a
 * cable [EXISTING_COST], every turn [TURN_COST] and every vertical cell [CLIMB_COST] more, so the
 * route prefers cables already laid, straight lines, and the ground. Every cell on the route must be [passable] (placeable or already a cable) and
 * [supported] (rests against some block), except [start] and [end], which only need to be passable.
 *
 * @return the cells from [start] to [end] inclusive, or null when there is no route within
 * [maxLength] cells or [maxVisited] states.
 */
fun findCablePath(
    start: Cell,
    end: Cell,
    passable: (Cell) -> Boolean,
    supported: (Cell) -> Boolean,
    existing: (Cell) -> Boolean = { false },
    maxLength: Int = CABLE_MAX_DISTANCE,
    maxVisited: Int = CABLE_PATH_MAX_VISITED,
): List<Cell>? {
    if (start == end) return listOf(start)
    if (!passable(start) || !passable(end)) return null

    val cameFrom: MutableMap<PathState, PathState> = hashMapOf()
    val best: MutableMap<PathState, Int> = hashMapOf()
    val queue: PriorityQueue<PathEntry> = PriorityQueue(compareBy(PathEntry::cost))
    val origin = PathState(start, null)
    best[origin] = 0
    queue.add(PathEntry(origin, 0, 1))
    var visited = 0

    while (queue.isNotEmpty() && visited < maxVisited) {
        val (state: PathState, cost: Int, length: Int) = queue.poll()
        if (cost > best.getValue(state)) continue
        visited++
        if (state.cell == end) return unwind(state, cameFrom)
        if (length >= maxLength) continue
        Cell.MOVES.forEach { move ->
            val next: Cell = state.cell + move
            if (next != end && (!passable(next) || !supported(next))) return@forEach
            val stepCost: Int = (if (existing(next)) EXISTING_COST else NEW_COST) +
                (if (state.move != null && state.move != move) TURN_COST else 0) +
                (if (move.y != 0) CLIMB_COST else 0)
            val nextState = PathState(next, move)
            val nextCost: Int = cost + stepCost
            if (nextCost >= (best[nextState] ?: Int.MAX_VALUE)) return@forEach
            best[nextState] = nextCost
            cameFrom[nextState] = state
            queue.add(PathEntry(nextState, nextCost, length + 1))
        }
    }
    return null
}

private data class PathState(val cell: Cell, val move: Cell?)
private data class PathEntry(val state: PathState, val cost: Int, val length: Int)

private fun unwind(last: PathState, cameFrom: Map<PathState, PathState>): List<Cell> {
    val path: MutableList<PathState> = mutableListOf(last)
    while (true) path.add(cameFrom[path.last()] ?: break)
    return path.asReversed().map(PathState::cell)
}

private const val NEW_COST: Int = 10
private const val EXISTING_COST: Int = 1
private const val TURN_COST: Int = 5

/** Per vertical cell, on top of [NEW_COST]: a run stays on the ground unless climbing saves real distance (route feedback 2026-08-30). */
private const val CLIMB_COST: Int = 4

/** Cap on how many states one lay will search before giving up, so a walled-off target never stalls a tick. */
const val CABLE_PATH_MAX_VISITED: Int = 4096
