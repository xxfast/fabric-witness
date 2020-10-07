package com.xfastgames.witness.utils

fun <T> Collection<T>.containsOnly(vararg others: T): Boolean =
    this.containsAll(others.toSet()) &&
            this.subtract(others.toList()).isEmpty()

/**
 * Zip the collection with itself where each entry is paired with the next element.
 * Pairs are returned in the order of first element [T] and second element [T]
 * @return listOf(first to second, second to third, third to fourth etc.)
 */
fun <T> Collection<T>.zipSelf(): List<Pair<T, T>> {
    val offset: List<T?> = listOf(null).plus(this)
    return offset.zip(this)
        .filter { (previous, _) -> previous != null }
        .filterIsInstance<Pair<T, T>>()
}

fun <T> Collection<T>.paired(): List<Pair<T, T>> =
    this.chunked(2)
        .map { items -> items.first() to items.last() }