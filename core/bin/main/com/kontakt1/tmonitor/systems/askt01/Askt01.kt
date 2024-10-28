package com.kontakt1.tmonitor.systems.askt01

import com.google.gson.Gson
import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.systems.System
import java.net.URL
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

    override suspend fun readAllStatesByRestApi(address: String?) {
        try {
            val json = URL("http://$address/api/v1/struct").readText()
            val downloadedSilos = Gson().fromJson(json, Array<Silo>::class.java)
            val downloadedCompleteParams = downloadedSilos.flatMap { it.params }
            silabus.listSilo.flatMap { it.params }.forEach { actualCompleteParam ->
                val downloadedActualCompleteParam = downloadedCompleteParams.find { it == actualCompleteParam}

                val lState = downloadedActualCompleteParam?.lParam?.state
                lState?.let { actualCompleteParam.lParam?.state = it }
                val ldUpState = downloadedActualCompleteParam?.ldUpParam?.state
                ldUpState?.let { actualCompleteParam.ldUpParam?.state = it }
                val ldDownState = downloadedActualCompleteParam?.ldDownParam?.state
                ldDownState?.let { actualCompleteParam.ldDownParam?.state = it }
                val tState = downloadedActualCompleteParam?.tParam?.state
                tState?.let { actualCompleteParam.tParam?.state = it }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun readLastTempIndications(
        connection: Connection,
        selectedParam: TParam
    ) = IndicationsASKT01Reader.readLastTempIndications(connection, selectedParam)

    override fun readMnemoschems(connection: Connection): List<Mnemoscheme>
            = StructASKT01Reader.readMnemoschems(connection)

    override suspend fun readAllStatesByJdbc(connection: Connection) : Boolean {
        return silabus.readAllStates(connection)
    }
}