package com.kontakt1.tmonitor.dataClasses.params.interfaces

import com.kontakt1.tmonitor.dataClasses.ConstraintApplication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.AnalogIndication
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Класс аналогого параметра. Используется в наследовании для параметров температуры и уровня.
 * @author Makarov V.G.
 */
abstract class AnalogParams<T : AnalogIndication?>(
    id:Int,
    alias:String,
    name:String,
    parent:Int,
    val constraintsList : List<ConstraintApplication>
) : Param<T>(id, alias, name, parent) {

    /**
     * Обновление последнего показания.
     */
    override suspend fun updateState(resultset: ResultSet?) {
        try {
            if (resultset != null) {
                //resultset.beforeFirst()
                while (resultset.next()) {
                    if(resultset.getInt("prm_id") == id) {
                        val constraintId = resultset.getInt("cnstr_id")
                        val constraintState = resultset.getInt("cnstr_state")
                        val saveTime = resultset.getTimestamp("cnstr_last_savetime")
                        saveTime ?: continue
                        val serverTime = Calendar.getInstance()
                        serverTime.timeInMillis = saveTime.time
                        //val serverTime = calculateSavetimeCalendar(saveTime)
                        val cnstr = constraintsList.find { it.constraint.id == constraintId }
                        if (isRelevantIndiacation(serverTime)) { // Показания есть и они актуальны
                            cnstr?.state = State.getByInt(constraintState)
                        } else {
                            cnstr?.state = State.OLD
                        }
                    }
                }
            }
            val lastState = state
            state = when {
                constraintsList.any { it.state == State.ALARM } -> State.ALARM
                constraintsList.any { it.state == State.OLD } -> State.OLD
                else -> State.OK
            }
            isNeedNotification = (lastState == State.OK && state != State.OK)
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}