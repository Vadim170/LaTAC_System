package com.kontakt1.tmonitor.systems

import com.google.gson.Gson
import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.utils.datetime.myRestDateTimeFormat
import com.kontakt1.tmonitor.utils.TimeCorrector
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import java.net.URL
//import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.Timestamp
import java.util.*

class DiagramSeries(
        val name: String,
        val data: List<Float?>
)

class DiagramData (
        val name: String,
        val title: String,
        val series: List<DiagramSeries>
)

class Series(
        val name: String,
        val data: List<List<Float>>
)
class ChartData (
        val name: String,
        val title: String,
        val series: List<Series>
)

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
        // TODO Мнемосхемы пока отключены
        val loadedMnemoschems = emptyList<Mnemoscheme>() // readMnemoschems(connection)
        mnemoschems.clear()
        mnemoschems.addAll(loadedMnemoschems)
        val silos = readSystemStruct(connection, numberAttempts)
        silabus.listSilo.clear()
        silabus.listSilo.addAll(silos)
    }

    /**
     *
     */
    suspend fun readStruct(address: String) {
        try {
            val json = URL("http://$address/api/v1/struct").readText()
            val silos = Gson().fromJson(json, Array<Silo>::class.java)
            silabus.listSilo.clear()
            silabus.listSilo.addAll(silos)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    suspend fun readIndicationsChart(address: String?,
                                     dateTimeFrom: Calendar,
                                     dateTimeTo: Calendar,
                                     selectedParam: Param<*>
    ) : List<Indication> {
        // TODO
        try {
            val type = when (selectedParam) {
                is LParam -> "Level"
                is TParam -> "Temperature"
                is LDUpParam -> "LevelDiscreteUp"
                is LDDownParam -> "LevelDiscreteDown"
                else -> return emptyList()
            }
            val url = "http://$address/api/v1/indications" +
                    "?paramType=$type" +
                    "&paramId=${selectedParam.id}" +
                    "&datetimefrom=${dateTimeFrom.myRestDateTimeFormat()}" +
                    "&datetimeto=${dateTimeTo.myRestDateTimeFormat()}"
            val str = synchronized(url) {
                try {
                    URL(url).readText()
                } catch (e:Exception) {
                    try {
                        URL(url).readText()
                    } catch (e:Exception) {
                        e.printStackTrace()
                        ""
                    }
                }
            }


            val chartData = Gson().fromJson(str, ChartData::class.java)
            val res = when(selectedParam) {
                is LParam -> {
                    chartData.series[0].data.map {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it[0].toLong()
                        LIndication(calendar, it[1])
                    }
                }
                is TParam -> {
                    // TODO Здесь происходит преобразование в Indication с массивом, а после при
                    //  отображении в массив показаний. Это надо переделать и везде хранить массив Indications
                    try {
                        val result = chartData.series[0].data.mapIndexed { index, list ->
                            val dt = chartData.series[0].data[index][0]
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = dt.toLong()
                            val tempArray = chartData.series.map {
                                val value = it.data[index][1]
                                value as Float?
                            }
                            val typedTempArray = tempArray.toTypedArray()
                            TIndication(calendar, typedTempArray)
                        }
                        result
                    } catch (e: Exception) {
                        emptyList<Indication>()
                    }
                }
                is LDUpParam -> {
                    chartData.series[0].data.map {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it[0].toLong()
                        DiscreteIndication(calendar, DiscreteIndication.DiscreteSensorState.getByInt(it[1].toInt()))
                    }
                }
                is LDDownParam -> {
                    chartData.series[0].data.map {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it[0].toLong()
                        DiscreteIndication(calendar, DiscreteIndication.DiscreteSensorState.getByInt(it[1].toInt()))
                    }
                }
                else -> emptyList()
            }
            return res
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    /**
     * Чтение данных для гистаграммы температур
     */
    abstract suspend fun readLastTempIndications(
        connection: Connection,
        selectedParam: TParam
    ): List<TIndication>

    /**
     * Чтение данных для гистаграммы температур через REST
     */
    suspend fun readLastTempIndications(
            address: String?,
            selectedParam: TParam
    ): List<TIndication> {
        try {
            val str = URL("http://$address/api/v1/indicationsLast" +
                    "?paramType=Temperature" +
                    "&paramId=${selectedParam.id}").readText()
            val diagramData = Gson().fromJson(str, DiagramData::class.java)
            return try {
                val tempArray = diagramData.series[0].data
                val typedTempArray = tempArray.toTypedArray()
                listOf(TIndication(Calendar.getInstance(), typedTempArray))
            } catch (e: Exception) {
                emptyList<TIndication>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

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

    fun getJSONSeriesOfDiagram(connection: Connection, param: Param<*>): String {
        return runBlocking {
            when (param) {
                is TParam -> {
                    val listIndications = readLastTempIndications(connection, param)
                    "[{ \"name\": \"Параметр ${param.alias}\", \"data\": ${listIndications.getOrNull(0)?.temp?.toList()} }]"
                }
                else -> ""
            }
        }
    }

    abstract suspend fun readAllStatesByRestApi(address: String?)

    fun clear() {
        silabus.listSilo.clear()
        selectedSilo = null
        selectedParam = null
    }

    fun findDataMnemoschemeOrEmpty(name: String): String {
        return mnemoschems.find { it.name == name }?.data ?: ""
    }

    abstract fun readMnemoschems(connection: Connection): List<Mnemoscheme>

    abstract suspend fun readAllStatesByJdbc(connection: Connection) : Boolean

    interface EventReadSilosUIListener {
        fun onUpdate()
        fun onPreLoad()
    }

    interface EventReadIndicationsUIListener {
        fun onPostExecuteReadIndicationsChart(param: Param<*>, isSuccess: Boolean, indications: List<Indication>?)
        fun onPreExecuteReadIndicationsChart()
    }

}