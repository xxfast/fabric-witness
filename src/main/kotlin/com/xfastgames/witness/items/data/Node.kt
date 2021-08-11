package com.xfastgames.witness.items.data

import com.google.common.graph.EndpointPair
import com.google.common.graph.MutableGraph
import com.google.common.graph.Traverser
import com.xfastgames.witness.utils.guava.clear
import net.minecraft.nbt.CompoundTag
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

private const val KEY_NODE_X = "x"
private const val KEY_NODE_Y = "y"
private const val KEY_NODE_MODIFIER = "modifier"

data class Node(val x: Float, val y: Float, val modifier: Modifier = Modifier.NONE)

fun CompoundTag.getNode() = Node(
    x = getFloat(KEY_NODE_X),
    y = getFloat(KEY_NODE_Y),
    modifier = getInt(KEY_NODE_MODIFIER)
        .let { Modifier.values()[it] }
)

fun CompoundTag.putNode(node: Node) {
    putFloat(KEY_NODE_X, node.x)
    putFloat(KEY_NODE_Y, node.y)
    putInt(KEY_NODE_MODIFIER, node.modifier.ordinal)
}

fun distance(u: Node, v: Node): Float =
    sqrt((v.x - u.x).pow(2) + (v.y - u.y).pow(2))

@Suppress("UnstableApiUsage")
fun distance(node: Node, edge: EndpointPair<Node>): Float {
    return TODO()
}

@Suppress("UnstableApiUsage")
operator fun EndpointPair<Node>.contains(node: Node) = this.nodeU() == node || this.nodeV() == node


/**
 * Optimize a node graph with [Ramer–Douglas–Peucker algorithm](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm)
 */
@Suppress("UnstableApiUsage")
fun MutableGraph<Node>.optimize(starting: Node): MutableGraph<Node> {
    val line: List<Node> = Traverser.forGraph(this).breadthFirst(starting).toList()
    if (line.size < 2) return this

    this.clear()

    val mutableList: MutableList<Node> = line.toMutableList()
    RamerDouglasPeucker(mutableList, 1.0, mutableList)

    mutableList.chunked(2).forEach { (prevNode, nexNode) ->
        putEdge(prevNode, nexNode)
    }
    return this
}

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

fun RamerDouglasPeucker(pointList: List<Node>, epsilon: Double, out: MutableList<Node>) {
    if (pointList.isEmpty()) return
    if (pointList.size < 2) throw IllegalArgumentException("Not enough points to simplify")

    // Find the point with the maximum distance from line between start and end
    var dmax = 0.0f
    var index = 0
    val end = pointList.size - 1
    for (i in 1 until end) {
        val d = perpendicularDistance(pointList[i], pointList[0], pointList[end])
        if (d > dmax) {
            index = i; dmax = d
        }
    }

    // If max distance is greater than epsilon, recursively simplify
    if (dmax > epsilon) {
        val recResults1 = mutableListOf<Node>()
        val recResults2 = mutableListOf<Node>()
        val firstLine = pointList.take(index + 1)
        val lastLine = pointList.drop(index)
        RamerDouglasPeucker(firstLine, epsilon, recResults1)
        RamerDouglasPeucker(lastLine, epsilon, recResults2)

        // build the result list
        out.addAll(recResults1.take(recResults1.size - 1))
        out.addAll(recResults2)
        if (out.size < 2) throw RuntimeException("Problem assembling output")
    } else {
        // Just return start and end points
        out.clear()
        pointList.firstOrNull()?.let { out.add(it) }
        pointList.lastOrNull()?.let { out.add(it) }
    }
}