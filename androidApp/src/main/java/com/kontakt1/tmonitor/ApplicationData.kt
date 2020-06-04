/*
 * Разработка мобильного приложения для системы АСКТ-01
 * Макаров В.Г. ст.гр.644 направление: 09.03.03
 * Жулева С.Ю. ст. преподаватель РГРТУ
 * MySQL Front
 * В этом файле описан главный статический класс приложения, в нем хранится состояние запущенного приложения.
 * Дата разработки: 16.04.2020
 */
package com.kontakt1.tmonitor

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.kontakt1.tmonitor.asyncTasks.Connect
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.settings.SettingsController
import com.kontakt1.tmonitor.systems.System
import com.kontakt1.tmonitor.systems.askt01.Askt01
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.sql.Connection
import java.util.*


/**
 * Главный статический класс приложения. Хранит в себе ссфлку на настройки, ссылки на оббработчики событий для UI,
 * запускает фоновые потоки, управляет логикой установки соединенияя, загрузки данных.
 * @author Makarov V.G.
 */
object ApplicationData {
    private const val NUMBER_ATTEMPTS_AUTOCONNECT = 20 // Колличество попыток автоподключения к базе данных
    private const val NUMBER_ATTEMPTS_READ_SILOS = 5 // Колличество попыток чтения силоса
    private const val SETTINGS_NAME = "settings" // Название файла настроек

    var serviceIsRunning = false // Флаг работы потока обновления состояний
        set(value) {
            field = value
            if(value) runJdbcReadAllLastIndicationsThread()
        }

    var settingsController: SettingsController? = null // Задаю в сеттере context

    var system: System = Askt01()           // Система
    var connection : Connection? = null     // Подключение

    // Это слабая ссылка на обработчик событий для силкорпуса, вызывается когда нужно обновить фрагмент на главном окне
    // Слабая потому, что нужно освободить память от обработчика, если форма не открыта. Обработчик задается
    // при открытии фрагмента, а вызывается при подключении к бд
    var silabusListenerUI = WeakReference<com.kontakt1.tmonitor.systems.System.EventReadSilosUIListener>(null)
    var silabusListenerService = WeakReference<com.kontakt1.tmonitor.systems.System.EventReadSilosUIListener>(null)

    // Создаем слабую ссылку (WeakReference) на обработчик для графического интерфейса, так как к моменту
    // завершения потока обработчика для интерфейса может и не быть и тогда его нужно освободить
    // (не держать ссылкой) для очстки памяти.
    var connectListenerUI = WeakReference<Connect.EventListenerForInterface>(null)
    var connectListenerService = WeakReference<Connect.EventListenerForInterface>(null)

    // Это слабая ссылка на обработчик событий для силкорпуса, вызывается когда нужно обновить фрагмент на главном окне
    // Слабая потому, что нужно освободить память от обработчика, если форма не открыта. Обработчик задается
    // при открытии фрагмента, а вызывается при подключении к бд
    var indicationsAllReadListenerUI = WeakReference<Silabus.EventListenerForInterfaceReadAllStates>(null)
    var indicationsAllReadListenerService = WeakReference<Silabus.EventListenerForInterfaceReadAllStates>(null)

    /**
     * Запуск фонового сервиса. Запустится только если сервис включен в настройках.
     */
    fun tryLaunchSevice(context: Context) {
        initSettingsIfNotInited(context)
        val settings = settingsController
        if (settings != null) {
            if (settings.settingsData.isServiceEnabled) startService(context)
            else stopService(context)
        }
    }

    /**
     * Запуск фонового сервиса.
     * @param context контекст приложения
     */
    private fun startService(context: Context) {
        if (serviceIsRunning) return // Если уже работает, ничего не запускаем
        context.startService(Intent(context, ServiceBackgroundRefresh::class.java))
        serviceIsRunning = true // По этому флагу работает поток опроса состояний
    }

    /**
     * Остановка фонового сервиса.
     * @param context контекст приложения
     */
    private fun stopService(context: Context) {
        context.stopService(Intent(context, ServiceBackgroundRefresh::class.java))
        serviceIsRunning = false
    }

    /**
     * Выполнение автоматического подключение. Подключение будет устонавливаться только если автоподключение включено
     * в настройках и если подлючение ещё не установлено в даный момент.
     * @param context контекст приложения
     */
    fun autoConnect(context: Context) {
        // Если автозаполнение/авоподключение выключено, то и не пытаемся подключиться, чтобы не нагружать.
        // А в полях и так храняться пустые строки
        initSettingsIfNotInited(context)
        val settings = settingsController
        if (settings != null) {
            if (settings.settingsData.isAutofillOn)
                connectIfNotConnected(context, NUMBER_ATTEMPTS_AUTOCONNECT)
        }
    }

    /**
     * Выполняет заданное количество раз попытку подключения если подключение не установленно.
     * @param numberAttempts количество попыток установки подключения
     * @param context контекст приложения
     */
    fun connectIfNotConnected(context: Context, numberAttempts: Int? = null) {
        val isConnected = try {
            connection != null && !(connection?.isClosed!!) // NULL сразу поверяю
        } catch (e: NullPointerException) {
            false
        }
        if(!isConnected) // Если не подключено, подключаем
            connect(context, numberAttempts)
    }

    /**
     * Отчистка всех данных о соединении.
     * @param context контекст приложения
     */
    private fun clear(context: Context) {
        stopService(context)
        connection = null
        system.clear()
    }

    /**
     * Подключение к базе данных.
     * @param numberAttempts количество попыток установки подключения
     * @param context контекст приложения
     */
    fun connect(context: Context, numberAttempts: Int? = null) {
        if(settingsController?.settingsData?.useRestServer == true) {
            // TODO Исправить. Сюда(в функцию) не должна заходить программа, если включен REST
            readSilos(context)
            return
        }
        clear(context)
        GlobalScope.launch(Dispatchers.Main) {
            connectListenerUI.get()?.onPreExecuteConnect()
            connectListenerService.get()?.onPreExecuteConnect()
        }
        GlobalScope.launch {
            initSettingsIfNotInited(context)
            val settings = settingsController
            if (settings != null) {
                try {
                    connection = if(numberAttempts == null) Connect.connectMySQL(settings.settingsData)
                    else Connect.connectMySQL(settings.settingsData, numberAttempts)
                } catch (e:Exception) {
                    e.printStackTrace()
                    launch(Dispatchers.Main) {
                        val messge = e.cause?.message
                        val toast = Toast.makeText(context, messge, Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
            launch(Dispatchers.Main) {
                connectListenerUI.get()?.onPostExecuteConnect(connection != null)
                connectListenerService.get()?.onPostExecuteConnect(connection != null)
            }
            if (connection != null) {
                readSilos(context)
            }
        }
    }

    /**
     * Чтение силкорпуса.
     * @param context контекст приложения
     */
    fun readSilos(context: Context) {
        // Действия до начала загрузки
        system.clear()
        GlobalScope.launch(Dispatchers.Main) {
            silabusListenerUI.get()?.onPreLoad()
            silabusListenerService.get()?.onPreLoad()
        }
        if (connection?.isClosed == true) { // Если подключение пропало, то подключаемся. При этом будт ошибка но подключение восстановится.
            connect(context)
            return
        }
        GlobalScope.launch {
            // Процедура требующая времени
            val settingsData = settingsController?.settingsData
            val connection = connection
            if(settingsData != null) {
                //system.readStruct(connection, NUMBER_ATTEMPTS_READ_SILOS)
                if (!settingsData.useRestServer) {
                    if (connection != null)
                        system.readStruct(connection, NUMBER_ATTEMPTS_READ_SILOS)
                } else {
                    system.readStruct(settingsData.address)
                }
            }
            launch(Dispatchers.Main) {
                silabusListenerUI.get()?.onUpdate()
                silabusListenerService.get()?.onUpdate()
                tryLaunchSevice(context) // Мера предосторожности на случай, если сервис сам не запустился при включении телефона
            }
        }
    }

    /**
     * Чтение листа показаний
     * Ссылка на UI обработчик слабая и если пользователь уйдет с графика, то он не вызовется.
     * @param readIndicationsChartListenerUI ссылка на бработчик событий для UI
     * @param dateTimeFrom момент времени с которого начинается загрузка
     * @param dateTimeTo момент времени до которого загружаются данные
     * @param context контекст приложения
     */
    fun readIndications(
        context: Context?,
        indicationsReadIndicationsUIListenerReader: com.kontakt1.tmonitor.systems.System.EventReadIndicationsUIListener,
        dateTimeFrom: Calendar,
        dateTimeTo: Calendar,
        selectedParam: Param<*>
    ) {
        // Это слабая ссылка на обработчик событий для силкорпуса, вызывается когда нужно обновить фрагмент на главном окне
        // Слабая потому, что нужно освободить память от обработчика, если форма не открыта. Обработчик задается
        // при открытии фрагмента, а вызывается при подключении к бд
        var indicationsReadListenerUI = WeakReference<com.kontakt1.tmonitor.systems.System.EventReadIndicationsUIListener>(null)

        // Действия до начала загрузки
        GlobalScope.launch(Dispatchers.Main) {
            indicationsReadListenerUI = WeakReference(indicationsReadIndicationsUIListenerReader)
            indicationsReadListenerUI.get()?.onPreExecuteReadIndicationsChart()
        }
        if (connection?.isClosed == true && context != null) { // Если подключение пропало, то подключаемся. При этом будт ошибка но подключение восстановится.
            connect(context)
        }
        // TODO сделать ожидание подключения перед загрузкой показаний
        GlobalScope.launch {
            // Процедура требующая времени
            val result = async {
                if(settingsController?.settingsData?.useRestServer == true) {
                    val address = settingsController?.settingsData?.address
                    return@async system.readIndicationsChart(address, dateTimeFrom, dateTimeTo, selectedParam)
                } else {
                    connection?.let { connection ->
                        return@async system.readIndicationsChart(connection, dateTimeFrom, dateTimeTo, selectedParam)
                    }
                }
            }.await()
            // Обработка результата
            val isSuccess = result != null
            launch(Dispatchers.Main) {
                indicationsReadListenerUI.get()?.onPostExecuteReadIndicationsChart(selectedParam, isSuccess, result)
            }
        }
    }

    /**
     * Инициализация настроек приложения, если они не были иничиализированы
     */
    fun initSettingsIfNotInited(context: Context) {
        if(settingsController == null) {
            settingsController = SettingsController(
                    context.getSharedPreferences(
                        SETTINGS_NAME,
                        Context.MODE_PRIVATE
                    )
                )
            initSystem(context)
        }
    }

    /**
     * Сбросить состояния
     */
    fun resetStates() {
        system.silabus.resetState()
        indicationsAllReadListenerUI.get()?.onPostExecuteReadAllStates(false)
    }

    /**
     * Сохранить настройки
     */
    fun saveSettings(context: Context) {
        settingsController?.save()
        initSystem(context)
        connect(context)
    }

    /**
     * Инициализация используемой системы
     */
    private fun initSystem(context: Context) {
        val selectedSystem = settingsController!!.settingsData.selectedSystem
        system = when (selectedSystem) {
            context.resources.getStringArray(R.array.systems)[0] -> Askt01()
            else -> Askt01()
        }
    }

    /**
     * Чтение последних показаний температуры
     * Ссылка на UI обработчик слабая и если пользователь уйдет с графика, то он не вызовется.
     * @param readIndicationsChartListenerUI ссылка на бработчик событий для UI
     * @param context контекст приложения
     * @param selectedParam выбранный параметр
     */
    fun readLastTempIndications(context: Context?, indicationsReadIndicationsUIListenerReader: com.kontakt1.tmonitor.systems.System.EventReadIndicationsUIListener, selectedParam: Param<*>) {
        if(selectedParam !is TParam) return
        // Это слабая ссылка на обработчик событий для силкорпуса, вызывается когда нужно обновить фрагмент на главном окне
        // Слабая потому, что нужно освободить память от обработчика, если форма не открыта. Обработчик задается
        // при открытии фрагмента, а вызывается при подключении к бд
        var indicationsReadListenerUI = WeakReference<com.kontakt1.tmonitor.systems.System.EventReadIndicationsUIListener>(null)

        // Действия до начала загрузки
        GlobalScope.launch(Dispatchers.Main) {
            indicationsReadListenerUI = WeakReference(indicationsReadIndicationsUIListenerReader)
            indicationsReadListenerUI.get()?.onPreExecuteReadIndicationsChart()
        }
        if (connection?.isClosed == true && context != null) { // Если подключение пропало, то подключаемся. При этом будт ошибка но подключение восстановится.
            connect(context)
        }
        // TODO сделать ожидание подключения перед загрузкой показаний
        GlobalScope.launch {
            // Процедура требующая времени
            val result = async {
                connection?.let { connection ->
                    return@async system.readLastTempIndications(connection, selectedParam)
                }
            }.await()
            // Обработка результата
            val isSuccess = result != null
            launch(Dispatchers.Main) {
                indicationsReadListenerUI.get()?.onPostExecuteReadIndicationsChart(selectedParam, isSuccess, result)
            }
        }
    }

    /**
     * Обновление текущих показаний по всей структуре
     */
    fun runJdbcReadAllLastIndicationsThread() {
        GlobalScope.launch {
            while (serviceIsRunning && system.silabus.listSilo.isNotEmpty()) {
                val isNeedNotification = (connection?.let { system.readAllStatesByJdbc(it) }) ?: false
                launch(Dispatchers.Main) {
                    indicationsAllReadListenerUI.get()?.onPostExecuteReadAllStates(isNeedNotification)
                }
                indicationsAllReadListenerService.get()?.onPostExecuteReadAllStates(isNeedNotification)
                delay(900_000)
            }
        }
    }

    /**
     * Обновление состояний через REST API
     */
    fun updateStatesRest(context: Context) {
        resetStates()
        GlobalScope.launch {
            val address = settingsController?.settingsData?.address
            address.let { system.readAllStatesByRestApi(it) }
            launch(Dispatchers.Main) {
                indicationsAllReadListenerUI.get()?.onPostExecuteReadAllStates(false)
            }
        }
    }
}
