package com.kontakt1.tmonitor.dataClasses

import com.google.gson.Gson
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State

/**
 * Силос системы АСКТ-01, имеет поля: список комплексных параметров.
 * @author Makarov V.G.
 */
class Silo(val name:String,
           val dir:String,
           val params: List<CompleteParam>
) {
    /**
     * Свойство для подсчета количества параметров температуры в силосе.
     */
    val tParamCount : Int
        get() = params.count { it.tParam != null }

    /**
     * Свойство для подсчета количества параметров уровня в силосе.
     */
    val lParamCount : Int
        get() = params.count { it.lParam != null }

    /**
     * Свойство для подсчета количества комплексных параметров параметров в силосе.
     */
    val paramCount : Int
        get() = params.size

    val state : State
        get() = when {
            params.any { it.state == State.ALARM } -> State.ALARM
            params.any { it.state == State.OLD } -> State.OLD
            else -> State.OK
        }

    override fun toString(): String {
        return Gson().toJson(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Silo

        if (name != other.name) return false
        if (dir != other.dir) return false
        if (params != other.params) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + dir.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }
}