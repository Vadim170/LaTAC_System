package com.kontakt1.tmonitor.dataClasses.indications

import com.kontakt1.tmonitor.dataClasses.indications.interfaces.AnalogIndication
import java.util.Calendar

/**
 * Класс показания температурного параметра. Используется для хранения последних показаний и при построении графика.
 * @author Makarov V.G.
 */
data class TIndication(
    override val dateTime: Calendar,
    val temp: Array<Float?>
) : AnalogIndication {

    fun toString(i:Int) = "[ ${dateTime.timeInMillis}, ${temp[i]} ]"

    override fun equals(other: Any?): Boolean {
        (other as TIndication)
        return this.temp contentEquals other.temp &&
            this.dateTime == other.dateTime
    }

    override fun hashCode(): Int {
        return super.hashCode()
        // TODO Переопределить hashCode()
    }
}