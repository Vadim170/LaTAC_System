package com.kontakt1.tmonitor.dataClasses.params

import com.kontakt1.tmonitor.dataClasses.ConstraintApplication
import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.params.interfaces.AnalogParams

/**
 * Класс параметра уровня.
 * Имеет suspend метод для загрузки последних паказаний. Имеет поля из базы данных, поле с последним показанием
 * из базы данных и состояние с учетом включенных уставок.
 * @author Makarov V.G.
 */
class LParam(
    id:Int,
    alias:String,
    name:String,
    parent:Int,
    constraintsList : List<ConstraintApplication>,
    val range:Int
) : AnalogParams<LIndication?>(id, alias, name, parent, constraintsList) {
    override val numberMinutesRelevant = 30L

    /*
    override suspend fun updateIndications() {
        try {
            val stmt = ApplicationData.connection?.createStatement()
            val resultset = stmt?.executeQuery(
                "SELECT savetime, l FROM tmonitor.l$name ORDER BY savetime DESC LIMIT 1")
            if (resultset != null) {
                resultset.next()
                val saveTime = resultset.getTimestamp("savetime")
                val serverTime = calculateSavetimeCalendar(saveTime)
                lastIndication =
                    LIndication(
                        serverTime,
                        resultset.getFloat("l")
                    )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }*/
}
