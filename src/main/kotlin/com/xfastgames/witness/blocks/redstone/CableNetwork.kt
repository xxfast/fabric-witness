package com.xfastgames.witness.blocks.redstone

/**
 * The cable-only view of [walkNetwork] (rules/minecraft/06-cable.md): every link carries power
 * both ways and every step counts toward [maxDistance]. Kept as the pinned shape of a run;
 * `CableNetworkTests` holds it to that.
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
    val walk: NetworkWalk<T> = walkNetwork(
        start = start,
        links = neighbours,
        feeds = { _, _ -> true },
        isSource = isSource,
        decays = { _, _ -> true },
        maxDistance = maxDistance,
        maxVisited = maxVisited,
    )
    return CableWalk(walk.component, walk.powered.keys)
}

data class CableWalk<T>(val component: Set<T>, val lit: Set<T>)

/** How far power travels along a run from its nearest source, in blocks. */
const val CABLE_MAX_DISTANCE: Int = 128

/** Cap on how much of a network one change will look at, so a change never walks a whole map. */
const val CABLE_MAX_VISITED: Int = 512
