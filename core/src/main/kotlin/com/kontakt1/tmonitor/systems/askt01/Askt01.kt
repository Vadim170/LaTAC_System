package com.kontakt1.tmonitor.systems.askt01

import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.systems.System
import java.sql.Connection
import java.sql.Timestamp

class Askt01 : System() {
    override suspend fun readSystemStruct(connection: Connection, numberAttempts: Int)
            = StructASKT01Reader.read(connection,numberAttempts)

    override suspend fun readSystemIndicationsChart(
        connection: Connection,
        timestampFrom: Timestamp,
        timestampTo: Timestamp,
        selectedParam: Param<*>
    ) = IndicationsASKT01Reader.read(connection, timestampFrom, timestampTo, selectedParam)

    override suspend fun readLastTempIndications(
        connection: Connection,
        selectedParam: TParam
    ) = IndicationsASKT01Reader.readLastTempIndications(connection, selectedParam)

    override fun readMnemoschems(connection: Connection): List<Mnemoscheme>
            = StructASKT01Reader.readMnemoschems(connection)
}