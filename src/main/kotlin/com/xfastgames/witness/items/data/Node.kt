package com.xfastgames.witness.items.data

import net.minecraft.nbt.CompoundTag
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