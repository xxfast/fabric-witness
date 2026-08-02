package com.xfastgames.witness.items.data

/**
 * The role of a [Node], and the traversal state of an [Edge].
 *
 * Serialized by ordinal ([putNode], [putValueGraph]) and read back positionally, so this enum is
 * **append-only**. Removing or reordering a value silently rewrites every panel already saved in a
 * world: drop [DOT] and a saved `START` reads back as `END`, a saved `END` as `HIDDEN`, and a saved
 * `HIDDEN` is out of range. [DOT] is kept for exactly that reason and nothing writes it any more,
 * hexagons having moved to [Atom]. See rules/witness/04-hexagon-dots.md.
 */
enum class Modifier { NONE, NORMAL, BREAK, DOT, START, END, HIDDEN }

/**
 * A symbol drawn on a node or an edge, held separately from that node's role and that edge's
 * traversal state so a start point or a dead-end stub can carry one too.
 *
 * Atom identity only. A hexagon's colour on a symmetry panel and its size on a sound panel are
 * attributes of a hexagon, not different kinds of hexagon, so they become their own fields when
 * those rules land rather than new values here (rules/witness/04-hexagon-dots.md). Append-only, for
 * the same reason as [Modifier].
 */
enum class Atom { NONE, HEXAGON }

/** A grid edge: whether the line may travel along it, and what is drawn on it. */
data class Edge(
    val modifier: Modifier = Modifier.NORMAL,
    val symbol: Atom = Atom.NONE
) {
    companion object {
        val NONE = Edge(Modifier.NONE)
        val NORMAL = Edge(Modifier.NORMAL)
        val BREAK = Edge(Modifier.BREAK)
        val HIDDEN = Edge(Modifier.HIDDEN)
    }
}

/**
 * Decodes a stored ordinal, falling back to `NONE` rather than throwing on a value this version
 * doesn't know. Matches the tolerant NBT readers: a panel written by a future version loses the
 * modifier it couldn't express instead of failing the whole world load.
 */
fun Int.toModifier(): Modifier = Modifier.values().getOrElse(this) { Modifier.NONE }

/** As [toModifier], for symbols. */
fun Int.toSymbol(): Atom = Atom.values().getOrElse(this) { Atom.NONE }
