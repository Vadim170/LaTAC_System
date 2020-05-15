package com.kontakt1.tmonitor.dataClasses.params.interfaces

import com.google.gson.Gson
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import java.sql.ResultSet
import java.util.*

/**
 * Класс параметра. Используется в наследовании для дивкретных и аналоговых параметров.
 * @author Makarov V.G.
 */
abstract class Param<T : Indication?>(
    val id:Int,
    val alias:String,
    val name:String,
    val parent:Int
) {
    var state: State = State.OK
    var isNeedNotification = false
    abstract suspend fun updateState(resultSet: ResultSet)
    protected abstract val numberMinutesRelevant: Long

    fun isRelevantIndiacation(timeLastIndication : Calendar): Boolean {
        val timeInMillisInLastIndication = timeLastIndication.timeInMillis
        val millisecInMinute = 60_000
        val criticalActualTime = numberMinutesRelevant * millisecInMinute // Колличество миллисекунд в 30 минутах
        val nowTime = Calendar.getInstance()
        val nowTimeInMillis = nowTime.timeInMillis
        return (nowTimeInMillis - timeInMillisInLastIndication <= criticalActualTime)
    }

    fun resetState() {
        state = State.OK
        isNeedNotification = false
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}

/**
 * Состояние данных параметра.
 */
enum class State(var value: Int) {
    OK(1), // Аварийных ситуаций нет, данные не устарели.
    ALARM(0), // Аварийная ситуация.
    OLD(-1); // Данные устарели.

    companion object {
        fun getByInt(value: Int) = when {
            value == 0 -> OK
            value > 0 -> ALARM.apply { this.value = value } // Хз, зачем, но если данные есть, то буду считывать и хранить
            else -> OLD.apply { this.value = value }
        }
    }
}
