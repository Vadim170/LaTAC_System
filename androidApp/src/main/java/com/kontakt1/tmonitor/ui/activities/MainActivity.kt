/*
 * Разработка мобильного приложения для системы АСКТ-01
 * Макаров В.Г. ст.гр.644 направление: 09.03.03
 * Жулева С.Ю. ст. преподаватель РГРТУ
 * MySQL Front
 * В этом файле описан класс главной активности.
 * Дата разработки: 16.04.2020
 */
package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.MyFirebaseMessagingService
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.ChartFragment
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.DiagramFragment
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.SilabusFragment
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.SiloFragment


/**
 * Класс главной активности. Она открывается при запуска приложения, имеет кнопки для открытия активностей настроек и
 * сведений о приложении, имеет FrameLayout для переключения фрагментов отображающих силкорпус, силос, график.
 * @author Makarov V.G.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Перечисление для описания активного в данный момент фрагмента на главной активности.
     */
    enum class FragmentEnum {
        SILABUS,
        SILO,
        CHART,
        DIAGRAM
    }

    /**
     * Поле для хранения информации о том, какой фрагмент в данный момент расположен на глваной активности.
     */
    private var selectedFragment: FragmentEnum =
        FragmentEnum.SILABUS
        set(value) {
            when (value) {
                FragmentEnum.SILABUS -> loadFragment(silabusFragment)
                FragmentEnum.SILO -> loadFragment(siloFragment)
                FragmentEnum.CHART -> loadFragment(chartFragment)
                FragmentEnum.DIAGRAM -> loadFragment(diagramFragment)
            }
            if(value == FragmentEnum.CHART || value == FragmentEnum.DIAGRAM)
                supportActionBar?.hide();
            else supportActionBar?.show();
            field = value
        }

    /**
     * Метод для изменения текущего фрагмента.
     * @param fragment фрагмент на который необходимо заменить текущий фрагмент
     */
    private fun onChangeFragment(fragment: FragmentEnum) {
        selectedFragment = fragment
    }

    private val siloFragment = SiloFragment.newInstance()
    private val silabusFragment = SilabusFragment.newInstance()
    private val chartFragment = ChartFragment.newInstance()
    private val diagramFragment = DiagramFragment.newInstance()

    lateinit var receiver: BroadcastReceiver

    /**
     * Метод вызываемый после создания активности.
     * @param savedInstanceState сохраненное состояние
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Задаем ресурс описывающий верстку объетков на активности.
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // По умолчанию первая вкладка
        selectedFragment =
            FragmentEnum.SILABUS
        siloFragment.onChangeFragment = ::onChangeFragment // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        silabusFragment.onChangeFragment = ::onChangeFragment  // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        chartFragment.onChangeFragment = ::onChangeFragment     // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        diagramFragment.onChangeFragment = ::onChangeFragment  // "Даём фрагменту инструмент для смены себя на другой фрагмент"

        // Приемник для сообщений от Firebase
        receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent) {
                ApplicationData.updateStatesRest(this@MainActivity)

                val title = intent.getStringExtra("title")
                val message = intent.getStringExtra("message")
                AlertDialog
                        .Builder(this@MainActivity)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Ok") { dialogInterface: DialogInterface, i: Int -> }
                        .create().show()
            }
        }

        ApplicationData.initSettingsIfNotInited(applicationContext)
        ApplicationData.autoConnect(applicationContext)
    }

    /**
     * Запуск активности. Назначение приемника сообщений.
     */
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(MyFirebaseMessagingService.INTENT_ACTION_SEND_MESSAGE)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    /**
     * Остановка активности. Отвязка приемника сообщений.
     */
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    /**
     * Создание меню
     * @param menu меню приложения
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appmenu_menu, menu)
        return true
    }

    /**
     * Диалоговое окно при выходе.
     */
    override fun onBackPressed() {
        when {
            selectedFragment == FragmentEnum.DIAGRAM -> selectedFragment =
                FragmentEnum.SILO
            selectedFragment == FragmentEnum.CHART -> selectedFragment =
                FragmentEnum.SILO
            selectedFragment == FragmentEnum.SILO -> selectedFragment =
                FragmentEnum.SILABUS
            else -> AlertDialog.Builder(this)
                .setTitle("Выйти из приложения?")
                .setMessage("Вы действительно хотите выйти?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes) { arg0, arg1 -> super@MainActivity.onBackPressed() }.create()
                .show()
        }
    }

    /**
     * Обработчик нажатий кнопок меню (Меню скрыто, но я его не удалил)
     * @param item элемент меню на который было произведено нажатие
     */
    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        when (item.itemId){
            R.id.appmenu_connect -> {
                val intent = Intent(this, SettingsActivity::class.java)
                this.startActivityForResult(intent, ResultsActivity.CONNECT.ordinal)
                return true
            }
            R.id.appmenu_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                this.startActivityForResult(intent, ResultsActivity.ABOUT.ordinal)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Обработка завернъшения выполнения активностей.
     * @param requestCode код запроса активности
     * @param resultCode код завершения
     * @param data данные
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ResultsActivity.CONNECT.ordinal -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                    } // Соединение установлено
                    Activity.RESULT_CANCELED -> {
                    } // Вышли без подключения
                }
                selectedFragment = FragmentEnum.SILABUS
            }
            ResultsActivity.ABOUT.ordinal -> {}
        }
    }

    /**
     * Загрузчик фрагментов в главное окно
     * @param fragment фрагмент для загрузки в главное меню
     */
    private fun loadFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.flContent, fragment)
        fragmentTransaction.commit()
    }

    /**
     * Идентификаторы потоков активностей
     */
    enum class ResultsActivity {
        CONNECT,
        ABOUT
    }
}

