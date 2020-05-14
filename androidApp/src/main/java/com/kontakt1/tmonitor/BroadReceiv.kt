package com.kontakt1.tmonitor
// Класс для автозапуска службы

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context

/**
 * Этот ресивер ловит события включения и перезакрузки устройства, которые прописани manifest
 */
class BroadReceiv : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ApplicationData.initSettingsIfNotInited(context)
        ApplicationData.tryLaunchSevice(context)
    }
}