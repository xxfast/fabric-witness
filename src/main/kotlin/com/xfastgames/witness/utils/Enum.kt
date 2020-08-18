package com.xfastgames.witness.utils

inline fun <reified T : Enum<T>> T.next(): T {
    val values: Array<T> = enumValues()
    val nextOrdinal: Int = (ordinal + 1) % values.size
    return values[nextOrdinal]
}

inline fun <reified T : Enum<T>> T?.nextIn(vararg values: T?): T? {
    val index: Int = values.indexOf(this)
    val nextOrdinal: Int = (index + 1) % values.size
    return values[nextOrdinal]
}