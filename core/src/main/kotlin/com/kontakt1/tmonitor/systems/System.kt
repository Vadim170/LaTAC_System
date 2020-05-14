package com.kontakt1.tmonitor.systems

import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.utils.TimeCorrector
import kotlinx.coroutines.runBlocking
//import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.Timestamp
import java.util.*

//@Component
abstract class System {

    val silabus = Silabus()                            // Силкорпус
    val mnemoschems = mutableListOf<Mnemoscheme>()
    var selectedSilo : Silo? = null                     // Выбранный силос
    var selectedCompleteParam : CompleteParam? = null   // Выбранный параметр
    var selectedParam : Param<*>? = null                // Выбранный параметр

    /**
     * Чтение структуры
     */
    suspend fun readStruct(connection: Connection, numberAttempts: Int = 5) {
        TimeCorrector.readTimeCorrection(connection)
        val loadedMnemoschems = readMnemoschems(connection)
        mnemoschems.clear()
        mnemoschems.addAll(loadedMnemoschems)
        val silos = readSystemStruct(connection, numberAttempts)
        silabus.listSilo.clear()
        silabus.listSilo.addAll(silos)
    }

    /**
     * Чтение данных для графика показаний
     */
    suspend fun readIndicationsChart(connection: Connection,
                                     dateTimeFrom: Calendar,
                                     dateTimeTo: Calendar,
                                     selectedParam: Param<*>
    ) : List<Indication> {
        val timestampFrom = Timestamp(TimeCorrector.timeInMillisAtServerTimezone(dateTimeFrom))
        val timestampTo = Timestamp(TimeCorrector.timeInMillisAtServerTimezone(dateTimeTo))
        return readSystemIndicationsChart(connection, timestampFrom, timestampTo, selectedParam)
    }

    /**
     * Чтение данных для гистаграммы температур
     */
    abstract suspend fun readLastTempIndications(
        connection: Connection,
        selectedParam: TParam
    ): List<Indication>

    protected abstract suspend fun readSystemStruct(connection: Connection, numberAttempts: Int = 5): List<Silo>

    protected abstract suspend fun readSystemIndicationsChart(connection: Connection,
                                                              timestampFrom: Timestamp,
                                                              timestampTo: Timestamp,
                                                              selectedParam: Param<*>): List<Indication>

    fun getJSONListSeriesOfCharts(connection: Connection, dateTimeFrom: Calendar, dateTimeTo: Calendar, param: Param<*>): List<String> {
        return runBlocking {
            val listIndications = readIndicationsChart(connection, dateTimeFrom, dateTimeTo, param)
            return@runBlocking when (param) {
                is LParam -> listOf("{ \"name\": \"Уровень\", \"data\": ${listIndications} }")
                is TParam -> {
                    (0 until param.sensors).map { i: Int ->
                        val currentListIndication = listIndications.map { (it as TIndication).toString(i) }
                        "{ \"name\": \"Датчик ${i + 1}\", \"data\": ${currentListIndication} }"
                    }
                }
                is LDUpParam ->  listOf("{ \"name\": \"Дискретный верхний датчик\", \"data\": ${listIndications} }")
                is LDDownParam ->  listOf("{ \"name\": \"Дискретный нижний датчик\", \"data\": ${listIndications} }")
                else -> listOf()
            }
        }
    }

    fun clear() {
        silabus.listSilo.clear()
        selectedSilo = null
        selectedParam = null
    }

    fun findDataMnemoschemeOrEmpty(name: String): String {
        return mnemoschems.find { it.name == name }?.data ?: ""
    }

    abstract fun readMnemoschems(connection: Connection): List<Mnemoscheme>

    interface EventReadSilosUIListener {
        fun onUpdate()
        fun onPreLoad()
    }

    interface EventReadIndicationsUIListener {
        fun onPostExecuteReadIndicationsChart(param: Param<*>, isSuccess: Boolean, indications: List<Indication>?)
        fun onPreExecuteReadIndicationsChart()
    }

}