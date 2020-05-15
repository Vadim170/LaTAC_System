package com.kontakt1.tmonitor.dataClasses.params.interfaces

import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication.DiscreteSensorState.*
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * Класс дискретного параметра. Используется в наследовании для дивкретного верхнего и нижнего параметра.
 * @author Makarov V.G.
 */
abstract class DiscreteParams(
	id:Int,
	alias:String,
	name:String,
	parent:Int
) : Param<DiscreteIndication?>(id, alias, name, parent) {
	override val numberMinutesRelevant = NUMBER_MINUTES_RELEVANT
	protected abstract val nameColumnStateInDB: String

	override suspend fun updateState(resultsetLastIndications: ResultSet?) {
		val lastIndication = updateIndications(resultsetLastIndications)
		lastIndication?.let { indication ->
			val lastState = state
			state = when {
				isRelevantIndiacation(indication.dateTime) && indication.value == OK -> State.OK
				isRelevantIndiacation(indication.dateTime) && indication.value == ALARM -> State.ALARM
				isRelevantIndiacation(indication.dateTime) && indication.value == UNKNOWN -> State.ALARM
				!isRelevantIndiacation(indication.dateTime) -> State.OLD
				else -> State.ALARM
			}
			isNeedNotification = (lastState == State.OK && state != State.OK)
		}
	}

	suspend fun updateIndications(resultSetLastIndications: ResultSet?): DiscreteIndication? {
		try {
			if (resultSetLastIndications != null) {
				//resultSetLastIndications.beforeFirst()
				while (resultSetLastIndications.next()) {
					if (resultSetLastIndications.getInt("prm_id") == id) {
						val serverTime = Calendar.getInstance()
						serverTime.timeInMillis = resultSetLastIndications.getTimestamp("savetime").time
						//val serverTime =
						//	calculateSavetimeCalendar(resultSetLastIndications.getTimestamp("savetime"))
						return DiscreteIndication(
								serverTime,
								Companion.getByInt(resultSetLastIndications.getInt(nameColumnStateInDB))
							)
					}
				}
			}
		} catch (e: SQLException) {
			e.printStackTrace()
		}
		return null
	}

	companion object Discrete {
		const val NUMBER_MINUTES_RELEVANT = 30L
	}
}