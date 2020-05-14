package com.kontakt1.tmonitor.dataClasses.indications.interfaces

import java.util.Calendar

/**
 * Класс показания параметра. Используется для наследования другими типами показаний.
 * @author Makarov V.G.
 */
interface Indication {
    val dateTime: Calendar
}
