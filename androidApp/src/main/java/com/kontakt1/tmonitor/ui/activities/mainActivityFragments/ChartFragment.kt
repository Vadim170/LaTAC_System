package com.kontakt1.tmonitor.ui.activities.mainActivityFragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.ApplicationData.system
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.MainActivity
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.systems.System
import com.kontakt1.tmonitor.ui.utils.chart.adjustChart
import com.kontakt1.tmonitor.ui.utils.chart.adjustDiscreteChart
import com.kontakt1.tmonitor.ui.utils.chart.mySetData
import com.kontakt1.tmonitor.ui.utils.chart.mySetDiscreteData
import com.kontakt1.tmonitor.ui.utils.datetime.DateTimePickerDialog
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import com.kontakt1.tmonitor.ui.utils.switchButton.getIntByParam
import com.kontakt1.tmonitor.ui.utils.switchButton.getParamByNumberAndCompleteParam
import com.kontakt1.tmonitor.ui.utils.switchButton.getParamsEnableds
import com.kontakt1.tmonitor.ui.widgets.SwitchButton
import kotlinx.android.synthetic.main.fragment_chart.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


/**
 * Класс фрагмента для отображения данных по параметру. Этот фрагмент встраивается в MainActivity.
 * @author Makarov V.G.
 */
class ChartFragment : Fragment() {
    var onChangeFragment: ((MainActivity.FragmentEnum) -> Unit)? = null
    private lateinit var dateTimeFrom: Calendar
    private lateinit var dateTimeTo: Calendar
    var lastDateTimeUpdate: Long = Calendar.getInstance().timeInMillis

    /**
     * Наполняем форму элементами описанными в xml.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    /**
     * Задаем параметры по умоланию при создании формы.
     * Запускаем фоновый поток обновления данных.
     * Настраиваем синхронизацию двух графиков.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dateTimeFrom = Calendar.getInstance()
        dateTimeFrom.set(Calendar.DATE, dateTimeFrom.get(Calendar.DATE) - 1)
        dateTimeTo = Calendar.getInstance()
        btnDateTimeFrom?.setOnClickListener(::onClickDateTimeFrom)
        btnDateTimeTo?.setOnClickListener(::onClickDateTimeTo)
        btnToDiagram?.setOnClickListener(::onClickToDiagram)
        configSwitchButton()
        launchBackgroundRefreshing()
        chart?.setOnClickListener{ syncDiscreteChart() }
    }

    private fun syncDiscreteChart() {
        chartDiscreteLevel?.fitScreen() // Замена resetZoom(). resetZoom не работает.
        val newOffsetX = chart.lowestVisibleX - dateTimeFrom.timeInMillis
        //Log.i("paddingLeft", "${chart.paddingLeft}")
        chartDiscreteLevel?.zoom(
            chart.scaleX, 1f,
            newOffsetX, 1f,
            chartDiscreteLevel.axisLeft.axisDependency
        )
    }

    /**
     * Обработчик для кнопок переключения между параметрами
     */
    private val onClckSwitchButton = object : SwitchButton.OnChangeListener {
        @SuppressLint("SetTextI18n")
        override fun onChange(position: Int) {
            val selectedCompleteParam = system.selectedCompleteParam
            system.selectedParam =
                getParamByNumberAndCompleteParam(
                    position,
                    selectedCompleteParam
                )
            tvParamName?.text = "Параметр ${system.selectedParam?.alias}"
            val isTempSelected = position == 0
            btnToDiagram.visibility = if (isTempSelected) View.VISIBLE else View.GONE
            chartDiscreteLevel?.adjustDiscreteChart()
            chart?.adjustChart()
            onRefresh()
        }
    }

    /**
     * Выбирает кнопку соответсквующую текущему параметру
     * Выключает кнопки, которые отсутствуют в текущем комплексном параметре
     */
    private fun configSwitchButton() {
        switchButton?.setOnChangeListener(onClckSwitchButton)
        val selectedParam =
            getIntByParam(system.selectedParam)
        val paramEnableds =
            getParamsEnableds(system.selectedCompleteParam)
        switchButton?.setCurrentPosition(selectedParam)
        switchButton?.setEnabledButtons(paramEnableds)
    }

    /**
     * Процедура обновления показаний.
     */
    private val readIndicationsChartListenerUI = object :
        System.EventReadIndicationsUIListener {
        override fun onPostExecuteReadIndicationsChart(
            param: Param<*>,
            isSuccess: Boolean,
            indications: List<Indication>?
        ) {
            if (isSuccess && indications != null) {
                chart?.mySetData(system.selectedParam, indications, dateTimeFrom, dateTimeTo)
                updateLastRefreshTime()
                chart?.invalidate()
            }
        }
        override fun onPreExecuteReadIndicationsChart() {
            tvLastUpdate?.text = tvLastUpdate?.text.toString() + ".."
        }
    }

    private fun updateLastRefreshTime() {
        val nowTime = Calendar.getInstance()
        val isDisplayByServerTimezone =
                (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
        tvLastUpdate?.text = nowTime.myStringFormat(isDisplayByServerTimezone, showSeconds = true, oneLine = true)
    }

    /**
     * Процедура обновления показаний.
     */
    private val readDiscreteIndicationsChartListenerUI = object :
        System.EventReadIndicationsUIListener {
        override fun onPostExecuteReadIndicationsChart(
            param: Param<*>,
            isSuccess: Boolean,
            indications: List<Indication>?
        ) {
            if (isSuccess && indications != null) {
                system.selectedParam.let {
                    chartDiscreteLevel?.mySetDiscreteData(param, indications, dateTimeFrom, dateTimeTo)
                }
                chartDiscreteLevel?.invalidate()
                updateLastRefreshTime()
            }
        }
        override fun onPreExecuteReadIndicationsChart() {
            readIndicationsChartListenerUI.onPreExecuteReadIndicationsChart()
        }
    }

    private fun onRefresh() {
        lastDateTimeUpdate = Calendar.getInstance().timeInMillis
        val param = system.selectedParam
        when(param) {
            is LParam -> {
                ApplicationData.readIndications(context, readIndicationsChartListenerUI, dateTimeFrom, dateTimeTo, param)
                system.selectedCompleteParam?.ldUpParam?.let { ldUpParam ->
                    ApplicationData.readIndications(context, readDiscreteIndicationsChartListenerUI, dateTimeFrom, dateTimeTo, ldUpParam)
                }
                system.selectedCompleteParam?.ldDownParam?.let { ldDownParam ->
                    ApplicationData.readIndications(context, readDiscreteIndicationsChartListenerUI, dateTimeFrom, dateTimeTo, ldDownParam)
                }
            }
            is TParam -> ApplicationData.readIndications(context, readIndicationsChartListenerUI, dateTimeFrom, dateTimeTo, param)
        }
        val isDisplayByServerTimezone =
                (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
        btnDateTimeFrom?.text = dateTimeFrom.myStringFormat(isDisplayByServerTimezone)
        btnDateTimeTo?.text = dateTimeTo.myStringFormat(isDisplayByServerTimezone)
    }

    /**
     * Слушатель для обновления информации на кнопках при нажатии на них.
     */
    private var onSetDateTimeListener = object : DateTimePickerDialog.OnDateTimeSetListener {
        override fun onDateTimeSet() {
            onRefresh()
        }

        override fun onException(e: Exception) {
            AlertDialog.Builder(view?.context)
                .setTitle("Ошибка")
                .setMessage(e.message)
                .setNegativeButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun onClickDateTimeFrom(view: View) {
        val settings = ApplicationData.settingsController ?: return
        DateTimePickerDialog(
            context = view.context,
            dateTime = dateTimeFrom,
            onDateTimeSetListener = onSetDateTimeListener,
            defaultDateTime = dateTimeFrom,
            max = dateTimeTo,
            isWorkingByServerTimeZone = settings.settingsData.isDisplayTimeByServerTimeZone
        ).show()
    }

    private fun onClickDateTimeTo(view: View) {
        val settings = ApplicationData.settingsController ?: return
        DateTimePickerDialog(
            context = view.context,
            dateTime = dateTimeTo,
            onDateTimeSetListener = onSetDateTimeListener,
            defaultDateTime = Calendar.getInstance(),
            min = dateTimeFrom,
            isWorkingByServerTimeZone = settings.settingsData.isDisplayTimeByServerTimeZone
        ).show()
    }

    private fun onClickToDiagram(view: View) {
        onChangeFragment?.invoke(MainActivity.FragmentEnum.DIAGRAM) // Переходим на диаграмму
    }

    private fun launchBackgroundRefreshing() {
        GlobalScope.launch {
            while (context != null) {
                launch(Dispatchers.Main) {
                    updateData()
                }
                delay(SECONDS_WAIT_BERORE_REFRESH)
            }
        }
    }

    private fun updateData() {
        // Определим сколько времени прошло с правой границы графика, на сколько нужно сдвинуть
        val timeDifference = lastDateTimeUpdate - Calendar.getInstance().timeInMillis
        dateTimeFrom.timeInMillis = dateTimeFrom.timeInMillis - timeDifference
        dateTimeTo.timeInMillis = dateTimeTo.timeInMillis - timeDifference
        // Обновим время на кнопках, загрузим данные
        onRefresh()
    }

    companion object {
        const val MAX_T_Y = 100f
        const val MIN_T_Y = -40f
        const val SECONDS_WAIT_BERORE_REFRESH = 10_000L

        fun newInstance(): ChartFragment {
            return ChartFragment()
        }
    }
}