package com.kontakt1.tmonitor.ui.activities.mainActivityFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.kontakt1.tmonitor.*
import com.kontakt1.tmonitor.ui.activities.MainActivity
import com.kontakt1.tmonitor.asyncTasks.Connect
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.systems.System
import com.kontakt1.tmonitor.ui.visualAdapters.SiloAdapter
import kotlinx.android.synthetic.main.fragment_silabus.*
import java.lang.ref.WeakReference

/**
 * Класс фрагмента для отображения содержимого силкорпуса (списка силосов). Этот фрагмент встраивается в MainActivity.
 * @author Makarov V.G.
 */
class SilabusFragment : Fragment() {
    var onChangeFragment: ((MainActivity.FragmentEnum) -> Unit)? = null

    /**
     * Обработчик события чтения силкорпуса для интерфейса
     * Храним сильную ссылку на обраотчик событий для того, чтобы го было нельзя удалить, пока форма существует
     */
    private var silabusListenerUI = object :
        System.EventReadSilosUIListener {
        override fun onUpdate() {
            pbConnect?.visibility = ProgressBar.INVISIBLE
            tvActualTask?.text = ""
            setSiloAdapter()
        }
        override fun onPreLoad() {
            pbConnect?.visibility = ProgressBar.VISIBLE
            tvActualTask?.text = "Загрузка структуры хранилища"
        }
    }

    private var silabusConnectListener = object : Connect.EventListenerForInterface {
        override fun onPostExecuteConnect(isSuccess: Boolean) {
            pbConnect?.visibility = ProgressBar.INVISIBLE
            tvActualTask?.text = ""
        }
        override fun onPreExecuteConnect() {
            pbConnect?.visibility = ProgressBar.VISIBLE
            tvActualTask?.text = "Подключение к базе данных"
        }
    }

    private var indicationsAllReadListenerUI = object  : Silabus.EventListenerForInterfaceReadAllStates {
        override fun onPostExecuteReadAllStates(isNeedNotification: Boolean) {
            gvSilosMap.invalidate()
            gvSilosMap.invalidateViews()
        }
        override fun onPreExecuteReadAllStates() {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_silabus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gvSilosMap.onItemClickListener =
            AdapterView.OnItemClickListener { parent, v, position, id ->
                ApplicationData.system.selectedSilo = ApplicationData.system.silabus.listSilo[position]
                onChangeFragment?.invoke(MainActivity.FragmentEnum.SILO) // Переходим на силос
            }
        setSiloAdapter()
        adjustGridView()
        // Назначаем обработчик для соединения, чтобы иметь возможность обновить содержимое силкорпуса на экране
        ApplicationData.connectListenerUI = WeakReference(silabusConnectListener)
        //ApplicationData.connectListenerUI.get()?.onPostExecuteConnect() // Вызываем только что созданный обработчик для отрисовки
        ApplicationData.silabusListenerUI = WeakReference(silabusListenerUI)
        ApplicationData.silabusListenerUI.get()?.onUpdate() // Вызываем только что созданный обработчик для отрисовки
        ApplicationData.indicationsAllReadListenerUI = WeakReference(indicationsAllReadListenerUI)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ApplicationData.connectListenerUI = WeakReference<Connect.EventListenerForInterface>(null)
        ApplicationData.silabusListenerUI = WeakReference<System.EventReadSilosUIListener>(null)
        ApplicationData.indicationsAllReadListenerUI = WeakReference<Silabus.EventListenerForInterfaceReadAllStates>(null)
    }

    /**
     * Регулировка сетки силосов
     */
    private fun adjustGridView() {
        gvSilosMap.numColumns = GridView.AUTO_FIT
        //gvMain.numColumns = Companion.COUNT_COLUMN
        gvSilosMap.columnWidth = 300
        gvSilosMap.verticalSpacing =
            SPACING
        gvSilosMap.horizontalSpacing =
            SPACING
        gvSilosMap.stretchMode = GridView.STRETCH_COLUMN_WIDTH
    }

    /**
     * Заполнение  сетки силосов данными из ApplicationData
     */
    private fun setSiloAdapter() {
        gvSilosMap.adapter = SiloAdapter(
            gvSilosMap.context,
            R.layout.gv_item_silo,
            ApplicationData.system.silabus.listSilo
        )
    }

    companion object {
        private const val SPACING = 5
        //private val COUNT_COLUMN = 3

        fun newInstance(): SilabusFragment {
            return SilabusFragment()
        }
    }
}