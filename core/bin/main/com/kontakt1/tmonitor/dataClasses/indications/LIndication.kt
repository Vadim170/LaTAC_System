package com.kontakt1.tmonitor.dataClasses.indications

import com.kontakt1.tmonitor.dataClasses.indications.interfaces.AnalogIndication
import java.util.Calendar

/**
 * Класс показания параметра уровня. Используется для хранения последних показаний и при построении графика.
 * @author Makarov V.G.
 */
data class LIndication(
    override val dateTime: Calendar,
    val value: Float
) : AnalogIndication {
    override fun toString() = "[ ${dateTime.timeInMillis}, $value ]"
}
