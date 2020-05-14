package com.kontakt1.tmonitor.utils

/**
 * Поиск минимального значения в nullable Array
 * @author Makarov V.G.
 */
fun Array<out Float?>.min(): Float? {
    if (isEmpty()) return null
    var min = this[0]
    if (min != null) {
        if (min.isNaN()) return min
        for (i in 1..lastIndex) {
            val e = this[i]
            if (e != null) {
                if (e.isNaN()) return e
                if (min != null) {
                    if (min > e) min = e
                }
            }
        }
    }
    return min
}

/**
 * Поиск максимального значения в nullable Array
 * @author Makarov V.G.
 */
fun Array<out Float?>.max(): Float? {
    if (isEmpty()) return null
    var max = this[0]
    if (max != null) {
        if (max.isNaN()) return max
        for (i in 1..lastIndex) {
            val e = this[i]
            if (e != null) {
                if (e.isNaN()) return e
                if (max != null) {
                    if (max < e) max = e
                }
            }
        }
    }
    return max
}