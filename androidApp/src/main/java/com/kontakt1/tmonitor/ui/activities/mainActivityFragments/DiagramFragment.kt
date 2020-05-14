package com.kontakt1.tmonitor.ui.activities.mainActivityFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.ApplicationData.system
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.MainActivity
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.systems.System
import com.kontakt1.tmonitor.ui.utils.chart.adjustTempChart
import com.kontakt1.tmonitor.ui.utils.chart.mySetTempData
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import kotlinx.android.synthetic.main.fragment_diagram.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


/**
 * Класс фрагмента для отображения диаграммы. Этот фрагмент встраивается в MainActivity.
 * @author Makarov V.G.
 */
class DiagramFragment : Fragment() {
    private lateinit var lastUpdate: Calendar
    var onChangeFragment: ((MainActivity.FragmentEnum) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_diagram, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvParamName?.text = "Параметр ${ApplicationData.system.selectedParam?.alias}"
        btnToChart?.setOnClickListener(::onClickToChart)
        diagram?.adjustTempChart()
        launchBackgroundRefreshing()
    }

    private val readIndicationsChartListenerUI = object :
        System.EventReadIndicationsUIListener {
        override fun onPostExecuteReadIndicationsChart(param: Param<*>, isSuccess: Boolean, indications: List<Indication>?) {
            if (isSuccess && indications != null && indications.isNotEmpty()) {
                diagram?.mySetTempData(indications.first())
            }
            val nowTime = Calendar.getInstance()
            val isDisplayByServerTimezone =
                    (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
            tvLastUpdate?.text = nowTime.myStringFormat(isDisplayByServerTimezone, showSeconds = true, oneLine = true)
            diagram?.invalidate()
        }
        override fun onPreExecuteReadIndicationsChart() {
            tvLastUpdate?.text = "${tvLastUpdate?.text}..."
        }
    }

    /**
     * Процедура обновления показаний.
     */
    private fun onRefresh() {
        system.selectedParam?.let {
            ApplicationData.readLastTempIndications(
                context,
                readIndicationsChartListenerUI,
                selectedParam = it
            )
        }
    }

    private fun onClickToChart(view: View) {
        onChangeFragment?.invoke(MainActivity.FragmentEnum.CHART) // Переходим на график
    }

    private fun launchBackgroundRefreshing() {
        GlobalScope.launch {
            while (context != null) {
                launch(Dispatchers.Main) {
                    onRefresh()
                }
                delay(SECONDS_WAIT_BERORE_REFRESH)
            }
        }
    }

    companion object {
        const val MAX_T_Y = 100f
        const val MIN_T_Y = -40f
        const val SECONDS_WAIT_BERORE_REFRESH = 10_000L

        fun newInstance(): DiagramFragment {
            return DiagramFragment()
        }
    }
}