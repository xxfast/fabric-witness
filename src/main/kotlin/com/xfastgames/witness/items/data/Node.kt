package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import net.minecraft.nbt.NbtCompound
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

private const val KEY_NODE_X = "x"
private const val KEY_NODE_Y = "y"
private const val KEY_NODE_MODIFIER = "modifier"

data class Node(val x: Float, val y: Float, val modifier: Modifier = Modifier.NONE)

fun NbtCompound.getNode() = Node(
    x = getFloat(KEY_NODE_X),
    y = getFloat(KEY_NODE_Y),
    modifier = getInt(KEY_NODE_MODIFIER)
        .let { Modifier.values()[it] }
)

fun NbtCompound.putNode(node: Node) {
    putFloat(KEY_NODE_X, node.x)
    putFloat(KEY_NODE_Y, node.y)
    putInt(KEY_NODE_MODIFIER, node.modifier.ordinal)
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