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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Param<*>) return false

        if (id != other.id) return false
        if (alias != other.alias) return false
        if (name != other.name) return false
        if (parent != other.parent) return false
        if (numberMinutesRelevant != other.numberMinutesRelevant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + alias.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + parent
        result = 31 * result + numberMinutesRelevant.hashCode()
        return result
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
