package com.xfastgames.witness.blocks.redstone

/**
 * The walk that decides what is powered in a network of cables, frames and stands
 * (rules/minecraft/06-cable.md, "a walk, not a neighbour update"; 05-puzzle-frame.md, "power comes
 * from the source"). Pure over an abstract position so it runs in unit tests without Minecraft;
 * the blocks supply the neighbour and predicate functions.
 *
 * Power is reachability from a real source. First the component reachable from [start] over
 * [links] (undirected), bounded by [maxVisited] so a pathological build cannot stall the server.
 * Then a spread from every member that [isSource], in [order] so ties never depend on where the
 * walk started, along the links that [feeds] power from one member to the next, into members
 * that [canHold] it. A link that [decays] costs one step of [maxDistance]; any other resets the
 * count, so a cable run measures its length from the last frame or stand it left.
 *
 * Nothing in the network is ever a source for itself: a cable lit by a frame's output is reached
 * *after* that frame, so cutting the frame's own source darkens both. That is what removes the
 * latches a neighbour-update model cannot avoid.
 *
 * @return every member of the component, and for each powered member the member whose power it
 * carries: the source itself, or the last frame or stand the power passed through.
 */
fun <T> walkNetwork(
    start: T,
    links: (T) -> List<T>,
    feeds: (from: T, to: T) -> Boolean,
    isSource: (T) -> Boolean,
    canHold: (T) -> Boolean = { true },
    decays: (from: T, to: T) -> Boolean,
    order: Comparator<T> = Comparator { _, _ -> 0 },
    maxDistance: Int = CABLE_MAX_DISTANCE,
    maxVisited: Int = CABLE_MAX_VISITED,
): NetworkWalk<T> {
    val component: MutableSet<T> = linkedSetOf(start)
    val frontier: ArrayDeque<T> = ArrayDeque(listOf(start))
    while (frontier.isNotEmpty() && component.size < maxVisited) {
        val current: T = frontier.removeFirst()
        links(current).forEach { next ->
            if (component.size < maxVisited && component.add(next)) frontier.addLast(next)
        }
    }

    val sources: List<T> = component.filter { at -> canHold(at) && isSource(at) }.sortedWith(order)
    val distance: MutableMap<T, Int> = linkedMapOf()
    val origin: MutableMap<T, T> = linkedMapOf()
    // Shortest "steps since the last reset" first, then arrival order, so a member is settled at
    // its best distance before anything further along is looked at and ties never depend on the
    // start. A reset link hands out distance 0, so this is not a plain breadth-first ring.
    var arrivals = 0
    val spread = java.util.PriorityQueue<Triple<Int, Int, T>>(compareBy({ it.first }, { it.second }))
    sources.forEach { source ->
        distance[source] = 0
        origin[source] = source
        spread.add(Triple(0, arrivals++, source))
    }
    while (spread.isNotEmpty()) {
        val (at: Int, _, from: T) = spread.poll()
        if (distance[from] != at) continue
        links(from).forEach { to ->
            if (to !in component || !canHold(to) || !feeds(from, to)) return@forEach
            val resets: Boolean = !decays(from, to)
            val next: Int = if (resets) 0 else at + 1
            if (next > maxDistance) return@forEach
            val best: Int? = distance[to]
            if (best != null && best <= next) return@forEach
            distance[to] = next
            // First arrival names the origin: a one-end frame also emits back into the cable that
            // feeds it, and that must not recolour the input run as the frame's own output.
            if (to !in origin) origin[to] = if (resets) from else origin.getValue(from)
            spread.add(Triple(next, arrivals++, to))
        }
    }
    return NetworkWalk(component, origin)
}

/** [powered] maps each powered member to the source, frame or stand its power comes from. */
data class NetworkWalk<T>(val component: Set<T>, val powered: Map<T, T>)
