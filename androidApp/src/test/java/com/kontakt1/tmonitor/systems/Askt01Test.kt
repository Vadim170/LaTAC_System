package com.kontakt1.tmonitor.systems

import com.kontakt1.tmonitor.asyncTasks.Connect
import com.kontakt1.tmonitor.settings.Settings
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.systems.askt01.Askt01
import com.kontakt1.tmonitor.systems.askt01.IndicationsASKT01Reader
import com.kontakt1.tmonitor.utils.TimeCorrector
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.*
import java.sql.Timestamp
import java.util.*

/**
 * Тест подключения, загрузки данных.
 */
class Askt01Test {
    private val settingsDataNotUse = Settings(
        databaseName = "tmonitor",
        login = "mememe",
        password = "724f23c7",
        address = "db4free.net",
        port = 3306
    )
    private val settingsData = Settings(
        databaseName = "tmonitor_test",
        login = "root",
        password = "",
        address = "localhost",
        port = 3306
    )
    private val dateTimeFrom: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2020)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DATE, 1)
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 59)
    }
    private val dateTimeTo: Calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2020)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DATE, 1)
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 30)
    }
    val timezoneForTest = TimeZone.getTimeZone("GMT+05:00")
    private val countConnectionTests: Int = 10

    /*@Test
    fun setUp() = runBlocking {
        println(settingsData)
        val connection = Connect.connectMySQL(settingsData)
        if (connection != null) {
            println(connection.isClosed == false)
            val runner = ScriptRunner(connection, false, false)
            val file = "src/test/java/com/kontakt1/tmonitor/asyncTasks/tmonitor_test_init.sql"
            runner.runScript(BufferedReader(FileReader(file)))
        }
    }*/

    /**
     * Тестирование установки подключения.
     * Выполняется попытка установить несколько подключений.
     */
    @Test
    fun connectMySQL() {
        println(settingsData)
        var countSuccess = 0
        println("Установка подключения тестируется $countConnectionTests раз...")
        runBlocking {
            repeat(countConnectionTests) { numberAttempts ->
                launch(Dispatchers.Default) {
                    var result = ""
                    var timeConsumedMillis = 0L
                    val start = java.lang.System.currentTimeMillis()
                    try {
                        val connection =
                            Connect.connectMySQL(
                                settingsData
                            )
                        if (connection == null)
                            result = "null"
                        else {
                            countSuccess += 1
                            result = "ok"
                        }
                    } catch (e:Exception) {
                        result = "error"
                    }
                    val finish = java.lang.System.currentTimeMillis()
                    timeConsumedMillis = finish - start
                    println("Попытка ${numberAttempts+1}, поток ${Thread.currentThread().id} : '${result}' \n" +
                            "Выполнялась секунд: ${timeConsumedMillis/1000.0}")
                }
            }
        }
        assertEquals(countConnectionTests, countSuccess)
    }

    /**
     * Для выполнения этого теста необходима базаданных системы АСКТ-01 с струкртурой как минимум с одним параметром и
     * как миниму с одним показанием за последний месяц по каждому параметру.
     * Выполняется:
     * Подключение к базе данных(3 попытки), загрузка структуры хранилища, обновление показаний всех параметров в
     * структуре хранилища, загрузка по каждому параметру показаний за месяц.
     */
    @Test
    fun readSilos() = runBlocking {
        println(settingsData)
        val connection =
            Connect.connectMySQL(
                settingsData,
                3
            )
        assertNotNull(connection)
        connection?.let{
            val system = Askt01()
            system.readStruct(connection)
            assert(system.silabus.listSilo.isNotEmpty())

            system.silabus.listSilo.forEach {
                println(it.name)
                it.params.forEach {
                    it.lParam?.let {
                        println("${it}")
                    }
                    it.tParam?.let {
                        println("${it}")
                    }
                    it.ldUpParam?.let {
                        println("${it}")
                    }
                    it.ldDownParam?.let {
                        println("${it}")
                    }
                }
            }
        }
        Unit
    }

    @Test
    fun readParamStates() = runBlocking {
        val connection =
            Connect.connectMySQL(
                settingsData
            )
        connection?.let {
            val system = Askt01()
            system.readStruct(connection)
            assert(system.silabus.listSilo.isNotEmpty())
            system.silabus.readAllStates(connection)
            system.silabus.listSilo.forEach {
                println(it.name)
                it.params.forEach {
                    it.lParam?.let {
                        println("${it} - ${it.state}")
                    }
                    it.tParam?.let {
                        println("${it} - ${it.state}")
                    }
                    it.ldUpParam?.let {
                        println("${it} - ${it.state}")
                    }
                    it.ldDownParam?.let {
                        println("${it} - ${it.state}")
                    }
                }
            }
        }
        Unit
    }

    @Test
    fun readChartIndications() = runBlocking {
        val ps = PrintStream(java.lang.System.out, true, "cp1251");
        val realTimezone = TimeZone.getDefault()
        val isDisplayByServerTimezone =
                (settingsData.isDisplayTimeByServerTimeZone)
        ps.println("Real:\n" +
                "Time zone:\t\t\t${realTimezone.id}\n" +
                "Current date&time:\t${Calendar.getInstance().myStringFormat(isDisplayByServerTimezone,true, true)}\n" +
                "Date&Time from:\t\t${dateTimeFrom.myStringFormat(isDisplayByServerTimezone,true, true)}\n" +
                "Date&Time to:\t\t${dateTimeTo.myStringFormat(isDisplayByServerTimezone, true, true)}")
        dateTimeFrom.timeZone = timezoneForTest
        dateTimeTo.timeZone = timezoneForTest
        TimeZone.setDefault(timezoneForTest)
        val connection =
            Connect.connectMySQL(
                settingsData
            )
        ps.println("Test:\n" +
                "Time zone:\t\t\t\t\t\t${timezoneForTest.id}\n" +
                "Current date&time in this test:\t${Calendar.getInstance().myStringFormat(isDisplayByServerTimezone,true, true)}\n" +
                "Date&Time from in this test:\t${dateTimeFrom.myStringFormat(isDisplayByServerTimezone,true, true)}\n" +
                "Date&Time to in this test:\t\t${dateTimeTo.myStringFormat(isDisplayByServerTimezone,true, true)}")
        val timestampFrom = Timestamp(TimeCorrector.timeInMillisAtServerTimezone(dateTimeFrom))
        val timestampTo = Timestamp(TimeCorrector.timeInMillisAtServerTimezone(dateTimeTo))
        connection?.let {
            val system = Askt01()
            system.readStruct(connection)
            assert(system.silabus.listSilo.isNotEmpty())
            system.silabus.listSilo.forEach { silo ->
                ps.println(silo.name)
                silo.params.forEach { param ->
                    param.lParam?.let {
                        ps.println("${it}")
                        IndicationsASKT01Reader.read(connection, timestampFrom, timestampTo, it)
                            .map { it as LIndication }
                            .forEach {
                                ps.println("${it.dateTime.myStringFormat(isDisplayByServerTimezone,false, true)} - ${it.value}")
                            }
                    }
                    param.tParam?.let {
                        ps.println("${it}")
                        IndicationsASKT01Reader.read(connection, timestampFrom, timestampTo, it)
                            .map { it as TIndication }
                            .forEach {
                                ps.println("${it.dateTime.myStringFormat(isDisplayByServerTimezone,false, true)} - ${it.temp.toList()}")
                            }
                    }
                    param.ldUpParam?.let {
                        ps.println("${it}")
                        IndicationsASKT01Reader.read(connection, timestampFrom, timestampTo, it)
                            .map { it as DiscreteIndication }
                            .forEach {
                                ps.println("${it.dateTime.myStringFormat(isDisplayByServerTimezone,false, true)} - ${it.value}")
                            }
                    }
                    param.ldDownParam?.let {
                        ps.println("${it}")
                        IndicationsASKT01Reader.read(connection, timestampFrom, timestampTo, it)
                            .map { it as DiscreteIndication }
                            .forEach {
                                ps.println("${it.dateTime.myStringFormat(isDisplayByServerTimezone,false, true)} - ${it.value}")
                            }
                    }
                }
            }
        }
        TimeZone.setDefault(realTimezone)
        Unit
    }
}