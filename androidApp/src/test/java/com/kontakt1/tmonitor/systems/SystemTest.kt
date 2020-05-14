package com.kontakt1.tmonitor.systems

import com.kontakt1.tmonitor.systems.askt01.Askt01
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import java.util.*

class SystemTest {
    private val dateTimeFrom: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2019)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DATE, 1)
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 59)
    }
    private val dateTimeTo: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2020)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DATE, 2)
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 30)
    }

    @Test
    fun readStruct() {
        val system = Askt01()
        runBlocking {
            system.readStruct("127.0.0.1")
        }
        println(system.silabus.listSilo.toString())
    }

    @Test
    fun readIndicationsChart() {
        val system = Askt01()
        runBlocking {
            val address = "127.0.0.1"
            system.readStruct(address)
            val param = system.silabus.listSilo[0].params[0].tParam
            if(param != null) {
                val res = system.readIndicationsChart(address, dateTimeFrom, dateTimeTo, param)
                println(res)
            }
        }
    }
}