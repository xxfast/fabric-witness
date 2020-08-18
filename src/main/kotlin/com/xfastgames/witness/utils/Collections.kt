package com.xfastgames.witness.utils

fun <T> Collection<T>.containsOnly(vararg others: T): Boolean =
    this.containsAll(others.toSet()) &&
            this.subtract(others.toList()).isEmpty()

fun <T> Collection<Collection<T>>.coordinatesOf(item: T): Pair<Int, Int>? {
    forEachIndexed { x, row ->
        row.forEachIndexed { y, head ->
            if (head == item) return x to y
        }
    }
    return null
}