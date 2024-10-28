package com.kontakt1.tmonitor.systems.askubuk01

import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import java.lang.Exception
import java.sql.*
import java.util.*

class IndicationsAskuBUK01Reader {
    companion object {
        suspend fun read(
            connection: Connection,
            timestampFrom: Timestamp,
            timestampTo: Timestamp,
            selectedParam: Param<*>
        ): List<Indication> {
            val emptyListForReturn = listOf<Indication>()
            if (connection.isClosed) return emptyListForReturn
            val stmt = connection.createStatement()

            return try {
                when (selectedParam) {
                    is LParam -> loadLIndications(
                        stmt,
                        selectedParam,
                        timestampFrom,
                        timestampTo
                    )
                    else -> emptyListForReturn
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyListForReturn // Возвращение пустого листа будет считаться ошибкой загрузки данных
            }
        }

        /**
         * Шаблон для функций загрузки показаний.
         */
        private fun loadIndications(
            stmt: Statement,
            request: String,
            body: (resultset: ResultSet, saveTime: Calendar) -> Indication
        ): List<Indication> {
            val result = mutableListOf<Indication>()
            val resultset = stmt.executeQuery(request)
            while (resultset.next()) {
                val savetimeCalendar = Calendar.getInstance()
                savetimeCalendar.timeInMillis = resultset.getTimestamp("DT").time
                //val savetimeCalendar =
                //    calculateSavetimeCalendar(resultset.getTimestamp("DT"))
                result.add(body(resultset, savetimeCalendar))
            }
            return result
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
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestampTo.time
            val tablename = "202004Rez" +
                    String.format("%03d", selectedParam.id) +
                    "_LevelMetr"
            val request = "SELECT * FROM $tablename WHERE DT >" +
                    " '$timestampFrom' AND DT < '$timestampTo' ORDER BY DT" // LIMIT 9000
            return loadIndications(
                stmt,
                request
            ) { resultset, saveTime ->
                LIndication(saveTime, resultset.getFloat("V")/1000)
            }
        }
    }
}