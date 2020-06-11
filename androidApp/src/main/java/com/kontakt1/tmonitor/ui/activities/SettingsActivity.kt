package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.solver.GoalRow
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
        swConnectByRest.setOnCheckedChangeListener(::swConnectByRestOnChackedChange)
        // Назначение обработчиков нажатий по кнопкам
        if (settings != null) {
            swRemember.isChecked = settings.isAutofillOn
            cbServiceEnabled.isChecked = settings.isServiceEnabled
            swConnectByRest.isChecked = settings.useRestServer
            etFCMTopic.isEnabled = !settings.useRestServer
        }
        cbSubscribeFCM.setOnCheckedChangeListener(::cbSubscribeFCMOnChackedChange)
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

    private fun swConnectByRestOnChackedChange(cb: CompoundButton, state: Boolean) {
        if (state) {
            if (cbSubscribeFCM.isChecked)
                fcmSubscribe()
            spnrSelectedSystem.visibility = View.GONE
            etConnectPassword.visibility = View.GONE
            etConnectLogin.visibility = View.GONE
            etConnectPort.visibility = View.GONE
            etDatabaseName.visibility = View.GONE
            cbDefaultDBName.visibility = View.GONE
            cbDisplayTimeByServerTimeZone.visibility = View.GONE
            tvSelectedSystem.visibility = View.GONE
            tvPort.visibility = View.GONE
            tvLogin.visibility = View.GONE
            tvPassword.visibility = View.GONE
            tvDatabaseName.visibility = View.GONE
            tvConnectSeparator.visibility = View.GONE
            cbServiceEnabled.visibility = View.GONE

            etFCMTopic.visibility = View.VISIBLE
            tvFCMTopic.visibility = View.VISIBLE
            cbSubscribeFCM.visibility = View.VISIBLE
        } else {
            if (cbSubscribeFCM.isChecked)
                fcmUnsubscribe()
            spnrSelectedSystem.visibility = View.VISIBLE
            etConnectPassword.visibility = View.VISIBLE
            etConnectLogin.visibility = View.VISIBLE
            etConnectPort.visibility = View.VISIBLE
            etDatabaseName.visibility = View.VISIBLE
            etFCMTopic.visibility = View.VISIBLE
            cbDefaultDBName.visibility = View.VISIBLE
            cbDisplayTimeByServerTimeZone.visibility = View.VISIBLE
            tvSelectedSystem.visibility = View.VISIBLE
            tvPort.visibility = View.VISIBLE
            tvLogin.visibility = View.VISIBLE
            tvPassword.visibility = View.VISIBLE
            tvDatabaseName.visibility = View.VISIBLE
            tvConnectSeparator.visibility = View.VISIBLE
            cbServiceEnabled.visibility = View.VISIBLE

            etFCMTopic.visibility = View.GONE
            tvFCMTopic.visibility = View.GONE
            cbSubscribeFCM.visibility = View.GONE
        }
    }

    private fun cbSubscribeFCMOnChackedChange(cb: CompoundButton, state: Boolean) {
        if (state) {
            fcmSubscribe()
            etFCMTopic.isEnabled = false
        } else {
            fcmUnsubscribe()
            etFCMTopic.isEnabled = true
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
                useRestServer = swConnectByRest.isChecked
                subscribeFCM = cbSubscribeFCM.isChecked
                isServiceEnabled = cbServiceEnabled.isChecked
                isAutofillOn = swRemember.isChecked
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
                swConnectByRest.isChecked = useRestServer
                cbSubscribeFCM.isChecked = subscribeFCM
                cbServiceEnabled.isChecked = isServiceEnabled
                swRemember.isChecked = isAutofillOn
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