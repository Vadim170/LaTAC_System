package com.kontakt1.tmonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kontakt1.tmonitor.asyncTasks.Connect
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State
import com.kontakt1.tmonitor.systems.System
import java.lang.ref.WeakReference

class ServiceBackgroundRefresh : Service() {

    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Уведомлния о уставках",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Уведомления о срабатывании уставок"
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }
        startForeground(MAIN_NOTIFY_ID, getMainNotaficationBuilder().build())

        ApplicationData.initSettingsIfNotInited(applicationContext)
        ApplicationData.connectListenerService = WeakReference(connectListener)
        ApplicationData.silabusListenerService = WeakReference(silabusReadListener)
        ApplicationData.indicationsAllReadListenerService = WeakReference(indicationsAllReadListenerService)
        // Устанавливаем подключение, если его нет
        ApplicationData.connectIfNotConnected(applicationContext,Int.MAX_VALUE)
        // Сервис обязан подключиться но TODO что если нет интернета? попытки быстро пролетят?
    }

    override fun onBind(p0: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Обработчик процесса подключения для сервиса
     */
    private val connectListener =  object : Connect.EventListenerForInterface {
        override fun onPostExecuteConnect(isSuccess: Boolean) = refreshMainNotafication()
        override fun onPreExecuteConnect() = onPostExecuteConnect(false)
    }

    private val silabusReadListener = object :
        System.EventReadSilosUIListener {
        override fun onUpdate() = refreshMainNotafication()
        override fun onPreLoad() = refreshMainNotafication()
    }

    private fun getMainNotaficationBuilder() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_k1)
            .setContentTitle(
                if(ApplicationData.connection == null || ApplicationData.connection!!.isClosed)
                    "Нет подключения" else "Подключено")
            .setContentText(
                with(ApplicationData.system.silabus){
                    "${listSilo.size} силос(ов,а), $countAlarm в аварии, $countOld устарели"
                })

    private fun refreshMainNotafication() {
        val builder = getMainNotaficationBuilder()
        notificationManager.notify(MAIN_NOTIFY_ID, builder.build())
    }

    fun sendAlarmNotif() {
        /* Intent для ссылки на активность
        val notificationIntent = Intent(this, ConnectActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,0, notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )*/
        val text = notaficationText()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            //.setContentIntent(contentIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            // обязательные настройки
            .setSmallIcon(R.drawable.ic_icon_alarm)
            .setContentTitle("Внимание!")
            .setContentText(text) // Текст уведомления
            // необязательные настройки
            .setTicker("Внимание!")
        val notification = NotificationCompat.BigTextStyle(builder)
            .bigText(text)
            .build()
        notificationManager.notify(NOTIFY_ID, notification)
    }

    private fun notaficationText(): String = StringBuilder()
            .apply {
                val allParams = ApplicationData.system.silabus.listSilo.flatMap { it.params }
                val namesAlarmLevel = allParams.filter {
                    it.lParam?.state == State.ALARM ||
                            it.ldUpParam?.state == State.ALARM ||
                            it.ldDownParam?.state == State.ALARM }
                    .map { it.name }
                if(namesAlarmLevel.isNotEmpty()) appendln("Аварии уровня: $namesAlarmLevel")
                val namesAlarmTemp = allParams
                    .filter { it.tParam?.state == State.ALARM }
                    .map { it.name }
                if(namesAlarmTemp.isNotEmpty()) appendln("Аварии температуры: $namesAlarmTemp")
                val namesOldParams = allParams
                    .filter { it.tParam?.state == State.OLD ||
                            it.lParam?.state == State.OLD ||
                            it.ldUpParam?.state == State.OLD ||
                            it.ldDownParam?.state == State.OLD }
                    .map { it.name }
                if(namesOldParams.isNotEmpty()) appendln("Устаревшие данные: $namesOldParams")
            }.toString().trim()

    /**
     * Обработчик процесса чтения всех показаний
     */
    private var indicationsAllReadListenerService =  object : Silabus.EventListenerForInterfaceReadAllStates {
        override fun onPostExecuteReadAllStates(isNeedNotification: Boolean) {
            if(isNeedNotification) sendAlarmNotif()
            refreshMainNotafication()
        }
        override fun onPreExecuteReadAllStates() { }
    }

    companion object {
        // Идентификатор уведомления
        private const val MAIN_NOTIFY_ID = 777
        private const val NOTIFY_ID = 101

        private const val CHANNEL_ID = "Alarm_notification_channel"
    }
}
