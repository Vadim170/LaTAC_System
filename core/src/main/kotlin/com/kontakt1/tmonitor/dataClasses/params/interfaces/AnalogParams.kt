package com.kontakt1.tmonitor.dataClasses.params.interfaces

import com.kontakt1.tmonitor.dataClasses.ConstraintApplication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.AnalogIndication
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
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
    override suspend fun updateState(resultSet: ResultSet) {
        try {
            resultSet.beforeFirst()
            val lastState = state
            while (resultSet.next()) {
                if(resultSet.getInt("prm_id") == id) {
                    val constraintId = resultSet.getInt("cnstr_id")
                    val constraintState = resultSet.getInt("cnstr_state")
                    val lastSaveTime = resultSet.getTimestamp("cnstr_last_savetime")
                    lastSaveTime ?: continue
                    val serverLastSaveTime = Calendar.getInstance()
                    serverLastSaveTime.timeInMillis = lastSaveTime.time
                    //val serverTime = calculateSavetimeCalendar(saveTime)
                    val cnstr = constraintsList.find { it.constraint.id == constraintId }
                    if (isRelevantIndiacation(serverLastSaveTime)) { // Показания есть и они актуальны
                        cnstr?.state = State.getByInt(constraintState)
                    } else {
                        cnstr?.state = State.OLD
                    }
                }
            }
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