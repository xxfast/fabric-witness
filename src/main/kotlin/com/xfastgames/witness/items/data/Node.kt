package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.xfastgames.witness.utils.getFloatTolerant
import com.xfastgames.witness.utils.getIntTolerant
import net.minecraft.nbt.NbtCompound
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

private const val KEY_NODE_X = "x"
private const val KEY_NODE_Y = "y"
private const val KEY_NODE_MODIFIER = "modifier"
private const val KEY_NODE_SYMBOL = "symbol"

data class Node(
    val x: Float,
    val y: Float,
    val modifier: Modifier = Modifier.NONE,
    val symbol: Symbol = Symbol.NONE
)

fun NbtCompound.getNode(): Node {
    val modifier: Modifier = getIntTolerant(KEY_NODE_MODIFIER).toModifier()
    // Panels written before hexagons moved to their own field stored one as the node's modifier,
    // which cost the node its role. Read it back as a symbol on a roleless node; the key is absent
    // on those panels, so the tolerant read below would otherwise drop the hexagon entirely.
    if (modifier == Modifier.DOT) return Node(
        x = getFloatTolerant(KEY_NODE_X),
        y = getFloatTolerant(KEY_NODE_Y),
        modifier = Modifier.NONE,
        symbol = Symbol.HEXAGON
    )
    return Node(
        x = getFloatTolerant(KEY_NODE_X),
        y = getFloatTolerant(KEY_NODE_Y),
        modifier = modifier,
        symbol = getIntTolerant(KEY_NODE_SYMBOL).toSymbol()
    )
}

fun NbtCompound.putNode(node: Node) {
    putFloat(KEY_NODE_X, node.x)
    putFloat(KEY_NODE_Y, node.y)
    putInt(KEY_NODE_MODIFIER, node.modifier.ordinal)
    putInt(KEY_NODE_SYMBOL, node.symbol.ordinal)
}

fun distance(u: Node, v: Node): Float =
    sqrt((v.x - u.x).pow(2) + (v.y - u.y).pow(2))

@Suppress("UnstableApiUsage")
operator fun EndpointPair<Node>.contains(node: Node) = this.nodeU() == node || this.nodeV() == node

/** Copy pasta of https://www.rosettacode.org/wiki/Ramer-Douglas-Peucker_line_simplification#Kotlin **/
private fun perpendicularDistance(pt: Node, lineStart: Node, lineEnd: Node): Float {
    var dx: Float = lineEnd.x - lineStart.x
    var dy: Float = lineEnd.y - lineStart.y

    // Normalize
    val mag: Float = hypot(dx, dy)
    if (mag > 0.0) {
        dx /= mag; dy /= mag
    }
    val pvx: Float = pt.x - lineStart.x
    val pvy: Float = pt.y - lineStart.y

    // Get dot product (project pv onto normalized direction)
    val pvdot: Float = dx * pvx + dy * pvy

    // Scale line direction vector and substract it from pv
    val ax: Float = pvx - pvdot * dx
    val ay: Float = pvy - pvdot * dy

    return hypot(ax, ay)
}