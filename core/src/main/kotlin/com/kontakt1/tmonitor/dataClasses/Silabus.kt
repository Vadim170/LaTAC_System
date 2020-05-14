package com.kontakt1.tmonitor.dataClasses

import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State
import java.sql.Connection
import java.sql.ResultSet

class Silabus {
    val listSilo = mutableListOf<Silo>()
    val countAlarm: Int
        get() = listSilo.count { it.state == State.ALARM }

    val countOld: Int
        get() = listSilo.count { it.state == State.OLD }

    /*suspend fun readSilos(connection: Connection, numberAttempts: Int = 5) {
        val result = ReadSilosASKT01().read(connection, numberAttempts)
        listSilo.clear()
        listSilo.addAll(result)
    }*/

	/**
	 * @return isNeedNotification
	 */
    suspend fun readAllStates(connection: Connection): Boolean {
        try {
            val params = listSilo.flatMap { it.params }
            val resultsetLConstraints = getResultSetLConstraintsStates(connection)
            val resultsetTConstraints = getResultSetTConstraintsStates(connection)
            val ldupParams = params.asSequence()
                .map { it.ldUpParam }
                .filterNotNull()
                .toList()
            val resultsetLDUpConstraints = getResultSetLDUpConstraintsStates(connection,ldupParams)
            val lddownParams = params.asSequence()
                .map { it.ldDownParam }
                .filterNotNull()
                .toList()
            val resultsetLDDownConstraints = getResultSetDDownConstraintsStates(connection,lddownParams)
            params.forEach {
                it.updateStates(
                    resultsetLConstraints,
                    resultsetTConstraints,
                    resultsetLDUpConstraints,
                    resultsetLDDownConstraints
                )
            }
            connection.close()
            return params.any { it.isNeedNotification }
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }
	
	private fun getResultSetLConstraintsStates(connection: Connection): ResultSet? {
        val stmt = connection.createStatement()
        val result = stmt?.executeQuery("SELECT prm_id, cnstr_id, cnstr_state, cnstr_last_savetime FROM lcnstr")
        return result
    }
	
    private fun getResultSetTConstraintsStates(connection: Connection): ResultSet? {
        val stmt = connection.createStatement()
        val result = stmt?.executeQuery("SELECT prm_id, cnstr_id, cnstr_state, cnstr_last_savetime FROM tcnstr")
        return result
    }
	
	private fun getResultSetLDUpConstraintsStates(connection: Connection, ldupParams: List<LDUpParam>): ResultSet? {
		val stmt = connection.createStatement()
		val request = StringBuilder()
		request.append("SELECT * FROM ( ")
		ldupParams.forEachIndexed { index, ldUpParam ->
			if(index != 0) request.append("UNION ")
			request.append("(SELECT '${ldUpParam.id}' as prm_id, savetime, ld_up FROM ldup${ldUpParam.name} ORDER BY savetime DESC LIMIT 1) ")
		}
		request.append(") as lastIndications")
        val result = stmt?.executeQuery(request.toString())
        return result
	}
	
	private fun getResultSetDDownConstraintsStates(connection: Connection, lddownParams: List<LDDownParam>): ResultSet? {
		val stmt = connection.createStatement()
		val request = StringBuilder()
		request.append("SELECT * FROM (")
		lddownParams.forEachIndexed { index, ldDownParam ->
			if(index != 0) request.append("UNION ")
			request.append("(SELECT '${ldDownParam.id}' as prm_id, savetime, ld_down FROM lddown${ldDownParam.name} ORDER BY savetime DESC LIMIT 1) ")
		}
		request.append(") as lastIndications")
        val result = stmt?.executeQuery(request.toString())
        return result
	}
    
    fun resetState() {
        listSilo.flatMap { it.params }
            .forEach { it.resetParamStates() }
    }

    fun findLParamById(id: Int): LParam? =
            listSilo.flatMap { it.params }
                .map { it.lParam }
                .find { it?.id == id }

    fun findTParamById(id: Int): TParam? =
            listSilo.flatMap { it.params }
                .map { it.tParam }
                .find { it?.id == id }

    fun findLDUpParamById(id: Int): LDUpParam? =
            listSilo.flatMap { it.params }
                    .map { it.ldUpParam }
                    .find { it?.id == id }

    fun findLDDownParamById(id: Int): LDDownParam? =
            listSilo.flatMap { it.params }
                    .map { it.ldDownParam }
                    .find { it?.id == id }

    fun getParam(paramId: Int?, paramType: String) : Param<*>? {
        return paramId?.let {
            when (paramType) {
                "Level" -> {
                    findLParamById(it)
                }
                "Temperature" -> {
                    findTParamById(it)
                }
                "LevelDiscreteUp" -> {
                    findLDUpParamById(it)
                }
                "LevelDiscreteDown" -> {
                    findLDDownParamById(it)
                }
                else -> {
                    null
                }
            }
        }
    }

    interface EventListenerForInterfaceReadAllStates {
        fun onPostExecuteReadAllStates(isNeedNotification : Boolean)
        fun onPreExecuteReadAllStates()
    }
}
