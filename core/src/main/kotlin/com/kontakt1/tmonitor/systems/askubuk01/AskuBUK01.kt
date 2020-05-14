package com.kontakt1.tmonitor.systems.askubuk01

import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.systems.System
import java.sql.Connection
import java.sql.Timestamp

class AskuBUK01 : System() {
    override suspend fun readSystemStruct(
        connection: Connection,
        numberAttempts: Int
    ) = SilosAskuBUK01Reader.read(connection,numberAttempts)

    override suspend fun readSystemIndicationsChart(
        connection: Connection,
        timestampFrom: Timestamp,
        timestampTo: Timestamp,
        selectedParam: Param<*>
    ) = IndicationsAskuBUK01Reader.read(connection, timestampFrom, timestampTo, selectedParam)

    override fun readMnemoschems(connection: Connection) = listOf<Mnemoscheme>()

    override suspend fun readLastTempIndications(
        connection: Connection,
        selectedParam: TParam
    ) = emptyList<TIndication>()
}