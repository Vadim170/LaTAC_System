package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.kontakt1.tmonitor.ApplicationData
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

    enum class FragmentEnum {
        SILABUS,
        SILO,
        CHART,
        DIAGRAM
    }

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

    private fun onChangeFragment(fragment: FragmentEnum) {
        selectedFragment = fragment
    }

    private val siloFragment = SiloFragment.newInstance()
    private val silabusFragment = SilabusFragment.newInstance()
    private val chartFragment = ChartFragment.newInstance()
    private val diagramFragment = DiagramFragment.newInstance()

    //private val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        // По умолчанию первая вкладка
        selectedFragment =
            FragmentEnum.SILABUS
        siloFragment.onChangeFragment = ::onChangeFragment // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        silabusFragment.onChangeFragment = ::onChangeFragment  // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        chartFragment.onChangeFragment = ::onChangeFragment     // "Даём фрагменту инструмент для смены себя на другой фрагмент"
        diagramFragment.onChangeFragment = ::onChangeFragment  // "Даём фрагменту инструмент для смены себя на другой фрагмент"

        ApplicationData.initSettingsIfNotInited(applicationContext)
        ApplicationData.autoConnect(applicationContext)
    }

    /**
     * Создание меню
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ResultsActivity.CONNECT.ordinal ->
                when(resultCode) {
                    Activity.RESULT_OK -> {} // Соединение установлено
                    Activity.RESULT_CANCELED -> {} // Вышли без подключения
                }
            ResultsActivity.ABOUT.ordinal -> {}
        }
    }

    /**
     * Загрузчик фрагментов в главное окно
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

