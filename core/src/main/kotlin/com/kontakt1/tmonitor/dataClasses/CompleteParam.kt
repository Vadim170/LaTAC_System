package com.kontakt1.tmonitor.dataClasses

import com.google.gson.Gson
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State
import java.sql.ResultSet

/**
 * Комплексный параметр системы АСКТ-01, имеет поля: температуры, уровня, верхнего дискретного датчика,
 * нижнего дискретного датчика.
 * @author Makarov V.G.
 */
class CompleteParam(
    val name: String,
    val lParam: LParam? = null,
    val tParam: TParam? = null,
    val ldUpParam: LDUpParam? = null,
    val ldDownParam: LDDownParam? = null,
    val description: String = "", // ASKU_BUK01
    val enabled: Boolean = true, // ASKU_BUK01
    val rezType: String = "" // ASKU_BUK01 TODO Сделать enum
) {
    val hasDiscreteParams = (ldUpParam != null || ldDownParam != null)
    var state = State.OK
    var isNeedNotification = false

    /**
     * Обновление последних показаний по параметрам.
     */
    suspend fun updateStates(
		resultsetLConstraints: ResultSet?,
		resultsetTConstraints: ResultSet?,
		resultsetLDUpConstraints: ResultSet?,
		resultsetLDDownConstraints: ResultSet?
	) {
        resultsetLConstraints?.let { lParam?.updateState(it) }
        resultsetTConstraints?.let { tParam?.updateState(it) }
        resultsetLDUpConstraints?.let { ldUpParam?.updateState(it) }
        resultsetLDDownConstraints?.let { ldDownParam?.updateState(it) }
        refreshState()
    }

    fun resetParamStates() {
        lParam?.resetState()
        tParam?.resetState()
        ldUpParam?.resetState()
        ldDownParam?.resetState()
        refreshState()
    }

    private fun refreshState() {
        state = when {
            lParam?.state == State.ALARM || tParam?.state == State.ALARM ||
                    ldUpParam?.state == State.ALARM || ldDownParam?.state == State.ALARM ->
                State.ALARM
            lParam?.state == State.OLD || tParam?.state == State.OLD ||
                    ldUpParam?.state == State.OLD || ldDownParam?.state == State.OLD ->
                State.OLD
            else -> State.OK
        }
        isNeedNotification = lParam?.isNeedNotification == true ||
                tParam?.isNeedNotification == true ||
                ldUpParam?.isNeedNotification == true ||
                ldDownParam?.isNeedNotification == true
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}