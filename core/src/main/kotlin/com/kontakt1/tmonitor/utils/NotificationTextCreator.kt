package com.kontakt1.tmonitor.utils

import com.kontakt1.tmonitor.dataClasses.params.interfaces.State
import com.kontakt1.tmonitor.systems.System

class NotificationTextCreator {
    companion object {
        fun generateText(system: System): String = StringBuilder()
                .apply {
                    val allParams = system.silabus.listSilo.flatMap { it.params }
                    val namesAlarmLevel = allParams.filter {
                        it.lParam?.state == State.ALARM ||
                                it.ldUpParam?.state == State.ALARM ||
                                it.ldDownParam?.state == State.ALARM
                    }
                            .map { it.name }
                    if (namesAlarmLevel.isNotEmpty()) appendln("Аварии уровня: $namesAlarmLevel")
                    val namesAlarmTemp = allParams
                            .filter { it.tParam?.state == State.ALARM }
                            .map { it.name }
                    if (namesAlarmTemp.isNotEmpty()) appendln("Аварии температуры: $namesAlarmTemp")
                    val namesOldParams = allParams
                            .filter {
                                it.tParam?.state == State.OLD ||
                                        it.lParam?.state == State.OLD ||
                                        it.ldUpParam?.state == State.OLD ||
                                        it.ldDownParam?.state == State.OLD
                            }
                            .map { it.name }
                    if (namesOldParams.isNotEmpty()) appendln("Устаревшие данные: $namesOldParams")
                }.toString().trim()
    }
}