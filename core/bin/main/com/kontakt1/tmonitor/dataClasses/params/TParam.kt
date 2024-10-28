package com.kontakt1.tmonitor.dataClasses.params

import com.kontakt1.tmonitor.dataClasses.ConstraintApplication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.params.interfaces.AnalogParams

/**
 * Класс параметра температуры.
 * Имеет suspend метод для загрузки последних паказаний. Имеет поля из базы данных, поле с последним показанием
 * из базы данных и состояние с учетом включенных уставок.
 * @author Makarov V.G.
 */
class TParam(
    id:Int,
    alias:String,
    name:String,
    parent:Int,
    constraintsList : List<ConstraintApplication>,
    val sensors:Int
) : AnalogParams<TIndication?>(id, alias, name, parent, constraintsList) {
    override val numberMinutesRelevant = 60L

    /*
    override suspend fun updateIndications() {
        try {
            val stmt = ApplicationData.connection?.createStatement()
            val resultset = stmt?.executeQuery(
                "SELECT * FROM tmonitor.t$name ORDER BY savetime DESC LIMIT 1")
            if (resultset != null) {
                resultset.next()
                val serverTime =
					calculateSavetimeCalendar(resultset.getTimestamp("savetime"))
                lastIndication =
                    TIndication(
                        serverTime,
                        Array(sensors) { i ->
                            try {
                                resultset.getString("t${i + 1}").toFloat()
                            } catch(e: SQLException) {
                                null
                            } catch(e: IllegalStateException) {
                                null
                            }// Если указано больше датчиков чем есть на самом деле. Возьмем MAX_VALUE (например в буке 19, а в базе написано 20)
                        }
                    )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }*/
}
