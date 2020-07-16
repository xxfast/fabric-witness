package com.xfastgames.witness.utils

fun <T> Collection<T>.containsOnly(vararg others: T): Boolean =
    this.containsAll(others.toSet()) &&
            this.subtract(others.toList()).isEmpty()
