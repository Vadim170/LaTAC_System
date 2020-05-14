package com.kontakt1.tmonitor.ui.utils.switchButton

import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param

fun getParamByNumberAndCompleteParam(value: Int, selectedCompleteParam: CompleteParam?): Param<*>? =
    when (value) {
        0 -> selectedCompleteParam?.tParam
        1 -> selectedCompleteParam?.lParam
        2 -> selectedCompleteParam?.lParam //ldUpParam
        3 -> selectedCompleteParam?.lParam //ldDownParam
        else -> null
    }

fun getIntByParam(param: Param<*>?) = when (param) {
    is TParam -> 0
    is LParam -> 1
    is LDUpParam -> 2
    is LDDownParam -> 3
    else -> 4
}

fun getParamsEnableds(completeParam: CompleteParam?) = arrayOf(
    completeParam?.tParam != null,
    completeParam?.lParam != null,
    completeParam?.ldUpParam != null,
    completeParam?.ldDownParam != null
)
