package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.R
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * Класс активности для подключения он-же пока используется для отображения настроек.
 * @author Makarov V.G.
 */
class SettingsActivity : AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val settings = ApplicationData.settingsController?.settingsData
        cbConnectByRest.setOnCheckedChangeListener(::cbConnectByRestOnChackedChange)
        // Назначение обработчиков нажатий по кнопкам
        if (settings != null) {
            cbRemember.isChecked = settings.isAutofillOn
            cbServiceEnabled.isChecked = settings.isServiceEnabled
            cbConnectByRest.isChecked = settings.useRestServer
            etFCMTopic.isEnabled = !settings.useRestServer
        }
        cbServiceEnabled.setOnCheckedChangeListener(::cbServiceEnabledOnChackedChange)
        cbDefaultDBName.setOnCheckedChangeListener(::cbDefaultDBNameOnChackedChange)
        spnrSelectedSystem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setDefDBNameIfNeed()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
        loadConnectSettings()
    }

    /**
     * Обработка закрытия активности через кнопку "Назад"
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
    }

    /**
     * Сохраняем настройки при выходе из формы
     */
    override fun onDestroy() {
        ApplicationData.settingsController?.save()
        updateConnectSettings()
        super.onDestroy()
    }

    /**
     * Изменение положения переключателя "Включить сервис"
     */
    private fun cbServiceEnabledOnChackedChange(cb: CompoundButton, state: Boolean) {
        updateConnectSettings()
        ApplicationData.tryLaunchSevice(applicationContext) // Мера предосторожности на случай, если сервис сам не запустился при включении телефона
        ApplicationData.resetStates()
    }

    private fun cbDefaultDBNameOnChackedChange(cb: CompoundButton, state: Boolean) {
        etDatabaseName.isEnabled = !state
        setDefDBNameIfNeed()
    }

    private fun cbConnectByRestOnChackedChange(cb: CompoundButton, state: Boolean) {
        if (!state) {
            fcmUnsubscribe()
            spnrSelectedSystem.isEnabled = true
            etConnectPassword.isEnabled = true
            etConnectLogin.isEnabled = true
            etConnectPort.isEnabled = true
            etDatabaseName.isEnabled = true
            etFCMTopic.isEnabled = true
            cbDefaultDBName.isEnabled = true
            cbServiceEnabled.isEnabled = true
        } else {
            fcmSubscribe()
            spnrSelectedSystem.isEnabled = false
            etConnectPassword.isEnabled = false
            etConnectLogin.isEnabled = false
            etConnectPort.isEnabled = false
            etDatabaseName.isEnabled = false
            etFCMTopic.isEnabled = false
            cbDefaultDBName.isEnabled = false
            cbServiceEnabled.isEnabled = false
        }
    }

    private fun fcmUnsubscribe() {
        val topic = etFCMTopic.text.toString()
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = getString(R.string.msg_unsubscribed, topic)
                    if (!task.isSuccessful) {
                        msg = getString(R.string.msg_subscribe_failed)
                    }
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
    }

    private fun fcmSubscribe() {
        val topic = etFCMTopic.text.toString()
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = getString(R.string.msg_subscribed, topic)
                    if (!task.isSuccessful) {
                        msg = getString(R.string.msg_subscribe_failed)
                    }
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
    }

    private fun setDefDBNameIfNeed() {
        if(cbDefaultDBName.isChecked) {
            val dafaultNames = resources.getStringArray(R.array.systems_default_db_names)
            val selectedDefName = dafaultNames.elementAtOrElse(spnrSelectedSystem.selectedItemPosition) { return }
            etDatabaseName.setText(selectedDefName)
        }
    }

    /**
     * Записывает параметры входа с формы в настройки подключения
     */
    private fun updateConnectSettings() {
        val settings = ApplicationData.settingsController
        if (settings != null) {
            val systemsNames = resources.getStringArray(R.array.systems)
            with(settings.settingsData) {
                selectedSystem = systemsNames[spnrSelectedSystem.selectedItemPosition]
                useRestServer = cbConnectByRest.isChecked
                isServiceEnabled = cbServiceEnabled.isChecked
                isAutofillOn = cbRemember.isChecked
                isDisplayTimeByServerTimeZone = cbDisplayTimeByServerTimeZone.isChecked
                isEnabledDefaultDBName = cbDefaultDBName.isChecked
                //ApplicationData.settings.databaseName =
                // Параметры ниже записываем, но они не будут сохранены, если автоподключение выключено
                login = etConnectLogin.text.toString()
                password = etConnectPassword.text.toString()
                address = etConnectAddress.text.toString()
                port = etConnectPort.text.toString().toInt()
                databaseName = etDatabaseName.text.toString()
                fcmtopic = etFCMTopic.text.toString()
            }
        }
        ApplicationData.saveSettings(applicationContext)
    }

    private fun loadConnectSettings() {
        ApplicationData.settingsController?.load()
        val settings = ApplicationData.settingsController
        if (settings != null) {
            val systemsNames = resources.getStringArray(R.array.systems)
            with(settings.settingsData) {
                val indexOfSelectedSystem = systemsNames.indexOfFirst { it == selectedSystem }
                spnrSelectedSystem.setSelection(indexOfSelectedSystem)
                cbConnectByRest.isChecked = useRestServer
                cbServiceEnabled.isChecked = isServiceEnabled
                cbRemember.isChecked = isAutofillOn
                cbDisplayTimeByServerTimeZone.isChecked = isDisplayTimeByServerTimeZone
                cbDefaultDBName.isChecked = isEnabledDefaultDBName
                etConnectLogin.setText(login)
                etConnectPassword.setText(password)
                etConnectAddress.setText(address)
                etConnectPort.setText(port.toString())
                etDatabaseName.setText(databaseName)
                etFCMTopic.setText(fcmtopic)
            }
        }
    }
}