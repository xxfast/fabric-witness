package com.xfastgames.witness.blocks.redstone

/**
 * The walk that decides which cables in a run are lit (rules/minecraft/06-cable.md). Pure over an
 * abstract position so it runs in unit tests without Minecraft; the block supplies the neighbour
 * and source functions.
 *
 * A signal with no decay cannot ride on neighbour updates alone, so on any change the whole
 * connected run is walked: first the component reachable from [start] over [neighbours], bounded
 * by [maxVisited] so a pathological build cannot stall the server, then a multi-source spread from
 * every cable in it that touches a source, bounded by [maxDistance] along the cable.
 *
 * @return every cable in the component, and the subset within [maxDistance] of a source.
 */
fun <T> walkCables(
    start: T,
    neighbours: (T) -> List<T>,
    isSource: (T) -> Boolean,
    maxDistance: Int = CABLE_MAX_DISTANCE,
    maxVisited: Int = CABLE_MAX_VISITED,
): CableWalk<T> {
    val component: MutableSet<T> = linkedSetOf(start)
    val frontier: ArrayDeque<T> = ArrayDeque(listOf(start))
    while (frontier.isNotEmpty() && component.size < maxVisited) {
        val current: T = frontier.removeFirst()
        neighbours(current).forEach { next ->
            if (component.size < maxVisited && component.add(next)) frontier.addLast(next)
        }
    }

    val sources: List<T> = component.filter(isSource)
    val lit: MutableSet<T> = sources.toMutableSet()
    var ring: List<T> = sources
    repeat(maxDistance) {
        if (ring.isEmpty()) return@repeat
        ring = ring.flatMap(neighbours).filter { next -> next in component && lit.add(next) }
    }
    return CableWalk(component, lit)
}

data class CableWalk<T>(val component: Set<T>, val lit: Set<T>)

/** How far power travels along a run from its nearest source, in blocks. */
const val CABLE_MAX_DISTANCE: Int = 128

/** Cap on how much of a run one change will look at, so a change never walks a whole map. */
const val CABLE_MAX_VISITED: Int = 512
