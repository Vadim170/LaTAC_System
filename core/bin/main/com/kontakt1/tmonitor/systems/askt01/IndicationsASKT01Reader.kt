package com.kontakt1.tmonitor.systems.askt01

import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.params.*
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import java.lang.Exception
import java.sql.*
import java.util.*

/**
 * Класс для загрузки показаний для графика.
 * @author Makarov V.G.
 */
class IndicationsASKT01Reader {
    companion object {
        suspend fun read(connection: Connection,
                         timestampFrom: Timestamp,
                         timestampTo: Timestamp,
                         selectedParam: Param<*>
        ): List<Indication> {
            val emptyListForReturn = listOf<Indication>()
            if(connection.isClosed) return emptyListForReturn
            val stmt =  connection.createStatement()
            return try {
                val result = when(selectedParam) {
                    is LParam -> loadLIndications(
                        stmt,
                        selectedParam,
                        timestampFrom,
                        timestampTo
                    )
                    is TParam -> loadTIndications(
                        stmt,
                        selectedParam,
                        timestampFrom,
                        timestampTo
                    )
                    is LDUpParam -> loadLDUpIndications(
                        stmt,
                        selectedParam,
                        timestampFrom,
                        timestampTo
                    )
                    is LDDownParam -> loadLDDownIndications(
                        stmt,
                        selectedParam,
                        timestampFrom,
                        timestampTo
                    )
                    else -> emptyListForReturn
                }
                stmt.close()
                result
            } catch (e: Exception) {
                stmt.close()
                e.printStackTrace()
                emptyListForReturn // Возвращение пустого листа будет считаться ошибкой загрузки данных
            }
        }

        suspend fun readLastTempIndications(connection: Connection,
                                            selectedParam: TParam): List<TIndication> {
            if(connection.isClosed) return listOf<TIndication>()
            val stmt =  connection.createStatement()
            return loadTIndications(stmt, selectedParam) as List<TIndication>
        }

        /**
         * Шаблон для функций загрузки показаний.
         */
        private fun loadIndications(stmt : Statement, request: String, body:(resultset: ResultSet, saveTime: Calendar) -> Indication) : List<Indication> {
            val result = mutableListOf<Indication>()
            val resultset = stmt.executeQuery(request)
            while (resultset.next()) {
                val savetimeCalendar = Calendar.getInstance()
                savetimeCalendar.timeInMillis = resultset.getTimestamp("savetime").time
                //val savetimeCalendar =
                //    calculateSavetimeCalendar(resultset.getTimestamp("savetime"))
                result.add(body(resultset,savetimeCalendar))
            }
            return result
        }

        /**
         * Загрузка листа показаний температуры
         */
        private suspend fun loadTIndications(
            stmt: Statement,
            selectedParam: TParam,
            timestampFrom: Timestamp? = null,
            timestampTo: Timestamp? = null
        ): List<Indication> {
            val request = if(timestampFrom == null || timestampTo == null)
                "SELECT * FROM t${selectedParam.name} ORDER BY savetime DESC LIMIT 1"
            else
                "SELECT * FROM t${selectedParam.name} WHERE savetime > '$timestampFrom' " +
                        "AND savetime < '$timestampTo' ORDER BY savetime"
            return loadIndications(
                stmt,
                request
            ) { resultset, saveTime ->
                TIndication(
                    saveTime,
                    Array(
                        selectedParam.sensors
                    ) { i ->
                        try {
                            resultset.getString("t${i + 1}").toFloat()
                        } catch (e: SQLException) {
                            Float.MAX_VALUE
                        } catch (e: IllegalStateException) {
                            Float.MAX_VALUE
                        }// Если указано больше датчиков чем есть на самом деле. Возьмем MAX_VALUE (например в буке 19, а в базе написано 20)
                    })
            }
        }

        /**
         * Загрузка листа показаний ldUp
         */
        private suspend fun loadLDUpIndications(
            stmt: Statement,
            selectedParam: LDUpParam,
            timestampFrom: Timestamp,
            timestampTo: Timestamp
        ): List<Indication> {
            return loadIndications(
                stmt,
                "SELECT * FROM ldup${selectedParam.name} WHERE savetime > '$timestampFrom' " +
                        "AND savetime < '$timestampTo' ORDER BY savetime" //  LIMIT 9000
            ) { resultset, saveTime ->
                DiscreteIndication(
                    saveTime,
                    DiscreteIndication.DiscreteSensorState.getByInt(resultset.getInt("ld_up"))
                )
            }
        }

        /**
         * Загрузка листа показаний ldDown
         */
        private suspend fun loadLDDownIndications(
            stmt: Statement,
            selectedParam: LDDownParam,
            timestampFrom: Timestamp,
            timestampTo: Timestamp
        ): List<Indication> {
            return loadIndications(
                stmt, "SELECT *  FROM lddown${selectedParam.name} WHERE savetime > '$timestampFrom' " +
                        "AND savetime < '$timestampTo' ORDER BY savetime" // LIMIT 9000
            ) { resultset, saveTime ->
                DiscreteIndication(
                    saveTime,
                    DiscreteIndication.DiscreteSensorState.getByInt(resultset.getInt("ld_down"))
                )
            }
        }

        /**
         * Загрузка листа показаний уровня
         */
        private suspend fun loadLIndications(
            stmt: Statement,
            selectedParam: LParam,
            timestampFrom: Timestamp,
            timestampTo: Timestamp
        ): List<Indication> {
            val request = "SELECT * FROM l${selectedParam.name} WHERE savetime >" +
                    " '$timestampFrom' AND savetime < '$timestampTo' ORDER BY savetime" // LIMIT 9000
            return loadIndications(
                stmt,
                request
            ) { resultset, saveTime ->
                LIndication(saveTime, resultset.getFloat("l"))
            }
        }
    }
}
