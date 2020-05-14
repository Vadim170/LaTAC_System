package com.kontakt1.tmonitor.dataClasses.indications

import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import java.util.*

/**
 * Класс показания дискретного параметра. Используется для хранения последних показаний и при построении графика.
 * @author Makarov V.G.
 */
data class DiscreteIndication(
    override val dateTime: Calendar,
    val value: DiscreteSensorState
) : Indication {

    override fun toString() = "[ ${dateTime.timeInMillis}, ${value.value} ]"

    enum class DiscreteSensorState(var value: Int) {
        OK(0), // Аварийных ситуаций нет, данные не устарели.
        ALARM(1), // Аварийная ситуация.
        UNKNOWN(2); // Данные неизвестны.

        fun getColor() =
            when(this) {
                OK -> 0xFF00FF00.toInt()
                ALARM -> 0xFFFF0000.toInt()
                else -> 0xFF555555.toInt()
            }

        companion object {
            fun getByInt(value: Int) = when {
                value == 0 -> OK
                value == 1 -> ALARM // Хз, зачем, но если данные есть, то буду считывать и хранить
                else -> UNKNOWN.apply { this.value = value }
            }
        }
    }
}
