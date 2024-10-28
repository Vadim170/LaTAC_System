package com.kontakt1.tmonitor.dataClasses

import com.google.gson.Gson
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State

/**
 * Класс примениения уставки.
 * Имеет ссылку на уставку и поле для её состояния. Используется в классах параметров.
 */
class ConstraintApplication(val constraint: Constraint,
                            var state: State = State.OLD) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

/**
 * Класс уставок для температуры и уровня.
 * @author Makarov V.G.
 */
class Constraint(
    val id: Int,
    val name: String,
    val using: Using,
    val value: Float,
    val insens: Float,
    val direction: Direction
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

/**
 * Напрвление уставок.
 */
enum class Direction(var value: Int) {
    UP(1),
    DOWN(-1),
    DISABLED(0);
    companion object {
        fun getByInt(value: Int) = when {
            value == 0 -> DISABLED
            value > 0 -> UP.apply { this.value = value } // Хз, зачем, но если данные есть, то буду считывать и хранить
            else -> DOWN.apply { this.value = value }
        }
    }
}

/**
 * Состояние использования уставок.
 */
enum class Using(var value: Int) {
    ENABLED(1),
    DISABLED(0);
    companion object {
        fun getByInt(value : Int) = when{
            value > 0 -> ENABLED.apply { this.value = value } // Хз, зачем, но если данные есть, то буду считывать и хранить
            else -> DISABLED.apply { this.value = value }
        }
    }
}
