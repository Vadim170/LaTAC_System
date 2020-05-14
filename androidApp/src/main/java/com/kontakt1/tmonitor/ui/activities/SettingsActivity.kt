package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.asyncTasks.Connect
import kotlinx.android.synthetic.main.activity_settings.*
import java.lang.ref.WeakReference

/**
 * Класс активности для подключения он-же пока используется для отображения настроек.
 * @author Makarov V.G.
 */
class SettingsActivity : AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val settings = ApplicationData.settingsController?.settingsData
        // Назначение обработчиков нажатий по кнопкам
        if (settings != null) {
            cbRemember.isChecked = settings.isAutofillOn
            cbServiceEnabled.isChecked = settings.isServiceEnabled
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
        super.onDestroy()
        ApplicationData.settingsController?.save()
        updateConnectSettings()
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
            }
        }
    }
}