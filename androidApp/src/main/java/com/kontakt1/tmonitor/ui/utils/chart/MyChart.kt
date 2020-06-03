package com.kontakt1.tmonitor.ui.utils.chart

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.ChartFragment
import com.kontakt1.tmonitor.dataClasses.ConstraintApplication
import com.kontakt1.tmonitor.dataClasses.Direction
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.LIndication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.params.*
import com.kontakt1.tmonitor.dataClasses.params.interfaces.AnalogParams
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.ui.utils.chart.markers.LvlMarkerView
import com.kontakt1.tmonitor.ui.utils.chart.markers.TempMarkerView
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import java.util.*
import kotlin.time.hours
import kotlin.time.milliseconds

/**
 * Цвета для графиков темперытуры в порядке нумеации датчиков.
 */
private val SENSORS_COLORS = arrayOf(
    0xFF000060, 0xFF0000A0, 0xFF0000ff, // 1  2  3
    0xFF008000, 0xFF00D000, 0xFF00ff00, // 4  5  6
    0xFF800000, 0xFFD00000, 0xFFF06060, // 7  8  9
    0xFF008080, 0xFF0080D0, 0xFF0080FF, // 10 11 12
    0xFF20FF80, 0xFF00FFD0, 0xFF00FFFF, // 13 14 15
    0xFF800080, 0xFF8000D0, 0xFF8000FF,
    0xFFFF0080, 0xFFFF00D0, 0xFFFF00FF,
    0xFF808000, 0xFF80D000, 0xFF80FF00,
    0xFFFF8000, 0xFFFFD000, 0xFFFFFF00,
    0xFFff0040, 0xFFFF8000, 0xFF4000ff
)

/**
 * Сброс всех настроек графика
 */
private fun LineChart.resetChart() {
    fitScreen()
    data?.clearValues()
    xAxis.valueFormatter = null
    notifyDataSetChanged()
    clear()
    invalidate()
}

/**
 * Регулировка внешнего вида графика, отрисовка полос уставок.
 */
fun LineChart.adjustChart() {
    val selectedParam = ApplicationData.system.selectedParam
    resetChart()
    configChartDesign(selectedParam)
    if(selectedParam != null) {
        // Если параметр аналоговый(Значит имеет уставки)
        if((selectedParam as? AnalogParams<*>) != null) {
            setConstraintLines(
                this.axisLeft,
                selectedParam.constraintsList
            ) // Настройка линий уставок
        } else {
            setConstraintLines(
                this.axisLeft,
                null
            ) // Настройка линий уставок
            this.resetZoom()
            this.isScaleYEnabled = false
        }
    }
    callOnClick()
}

/**
 * Установка данных в график LineChart
 * @param param параметр по которому необходимо отображать данные.
 * @param indications список показаний которые необходимо отобразить.
 * @param dateTimeFrom время с которого необходимо отобразить данные.
 * @param dateTimeTo время до которого необходимо отобразить данные.
 */
fun LineChart.mySetData(param: Param<*>?, indications: List<Indication>, dateTimeFrom: Calendar,
                            dateTimeTo: Calendar) {
    val dataSets = mutableListOf<LineDataSet>()
    val sensorsCount = when(param) {
        is TParam -> param.sensors
        is LParam, is LDUpParam, is LDDownParam -> 1
        else -> 0
    }
    for (sensor in sensorsCount-1 downTo 0) {
        val values = getValuesList(
            indications, sensor, dateTimeFrom,
            dateTimeTo
        )
        dataSets.add(
            getMyLineDataSet(
                values,
                sensor
            )
        )
    }
    dataSets.sortBy { it.label.toInt() } // Необходимо, чтобы номер датчика при нажатии на график отображался корректно
    val data = LineData(dataSets as List<ILineDataSet>)
    data.setDrawValues(false) // Отключаем подписи к точкам
    this.data = data
}

private fun getValuesList(indications: List<Indication>, sensor: Int, dateTimeFrom: Calendar,
                          dateTimeTo: Calendar) =
    mutableListOf<Entry>().apply {
        indications.forEachIndexed { index, indication ->
            val dateTime = indication.dateTime.timeInMillis
            val value = when (indication) {
                is LIndication -> indication.value
                is TIndication -> indication.temp[sensor]
                is DiscreteIndication -> indication.value.value.toFloat()
                else -> null
            }
            if(index == 0) {
                // Добавим псевдоточку для того чтобы можно было масштабировать график до ограничений по горизонтали
                add(Entry(dateTimeFrom.timeInMillis.toFloat(), value?:0f))
            }
            value?.let{
                add(Entry(dateTime.toFloat(), value))
            }
            if(index == indications.lastIndex) {
                // Добавим вторую псевдоточку для того чтобы можно было масштабировать график до ограничений по горизонтали
                add(Entry(dateTimeTo.timeInMillis.toFloat(), value?:0f))
            }
        }
    }

private fun getMyLineDataSet(values: List<Entry>, sensor: Int) =
    LineDataSet(values, "${sensor + 1}").apply {
        color = getSensorLineColor(sensor).toInt()
        setCircleColor(color)
        setDrawIcons(false)
        lineWidth = 1f // line thickness and point size
        circleRadius = 2f
        setDrawCircleHole(false) // draw points as solid circles
        formLineWidth = 1f // customize legend entry
        formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        formSize = 10f
        enableDashedHighlightLine(10f, 10f, 0f) // draw selection line as dashed
    }

internal fun setConstraintLines(yAxis: YAxis, constraintsList: List<ConstraintApplication>?, drawLabels: Boolean = true) {
    // draw limit lines behind data instead of on top
    yAxis.removeAllLimitLines()
    yAxis.setDrawLimitLinesBehindData(constraintsList != null)
    constraintsList?.map { it.constraint }
        ?.forEach { constraint ->
            val str = when {
                !drawLabels -> ""
                constraint.direction != Direction.DISABLED -> constraint.name
                else -> "Отключенная уставка"
            }
            val limitLine = LimitLine(constraint.value, str)
            limitLine.lineWidth = 2f
            limitLine.enableDashedLine(10f, 10f, 0f)
            limitLine.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
            limitLine.textSize = 10f
            limitLine.typeface = Typeface.SANS_SERIF // tfRegular
            yAxis.addLimitLine(limitLine) // add limit lines
        }
}

private fun LineChart.configChartDesign(selectedParam: Param<*>?) {
    isDragDecelerationEnabled = false // Отключаю скольжение. Нужно чтобы график дискретных датчиков правильно синхронизировался
    setNoDataText("")
    setNoDataTextColor(ContextCompat.getColor(context, R.color.stateAlarm))
    configChartXAxis(xAxis)
    configChartYAxis(axisLeft)
    legend.isEnabled = false
    //configChartLegend(legend)
    setBackgroundColor(Color.WHITE) // background color*/
    description.isEnabled = false // disable description text
    setTouchEnabled(true) // enable touch gestures
    //setOnChartValueSelectedListener(this) // set listeners
    setDrawGridBackground(false)
    // Задаём отрисовку маркеров
    setDrawMarkers(selectedParam is AnalogParams<*>?)
    marker = when (selectedParam) {
        is TParam -> TempMarkerView(
            context,
            R.layout.custom_marker_view
        )
        is LParam -> LvlMarkerView(
            context,
            R.layout.custom_marker_view
        )
        else -> null
    }
    isDragEnabled = true // enable scaling and dragging
    isScaleXEnabled = true
    isScaleYEnabled = true
    setPinchZoom(false) // force pinch zoom along both axis
    setDrawBorders(true) // Рисовать рамку
    extraLeftOffset = 15f // Сдвинем грануцу самого левого лейбла
    extraRightOffset = 15f // Сдвинем грануцу самого правого лейбла
    animateX(500) // draw points over time
    axisRight.isEnabled = false // disable dual axis (only use LEFT axis)
}

private fun configChartYAxis(yAxis: YAxis) {
    yAxis.enableGridDashedLine(10f, 0f, 0f) // horizontal grid lines
    val selectedParam = ApplicationData.system.selectedParam
    yAxis.axisMaximum = when (selectedParam) {
        is TParam -> ChartFragment.MAX_T_Y
        is LParam -> selectedParam.range.toFloat()
        else -> 2f
    }
    yAxis.axisMinimum = when (selectedParam) {
        is TParam -> ChartFragment.MIN_T_Y
        else -> 0f
    }
}

private fun configChartXAxis(xAxis: XAxis) {
    xAxis.enableGridDashedLine(10f, 0f, 0f) // vertical grid lines
    xAxis.labelRotationAngle = -9f
    //xAxis.setAvoidFirstLastClipping(false)
    //xAxis.isGranularityEnabled = true
    //xAxis.granularity = 50f
    xAxis.labelCount = 5
    // Формат вывода даты и времени
    xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val date = Calendar.getInstance()
            date.timeInMillis = value.toLong()
            val isDisplayByServerTimezone =
                    (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
            return date.myStringFormat(isDisplayByServerTimezone)
        }
    }
}

private fun configChartLegend(legend: Legend) {
    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
    legend.orientation = Legend.LegendOrientation.HORIZONTAL
    legend.setDrawInside(false)
    legend.typeface = Typeface.SANS_SERIF // tfLight
    legend.yOffset = 15f
    legend.xOffset = 0f
    legend.yEntrySpace = -1f // Расстояние между строками
    legend.textSize = 8f
    legend.form = Legend.LegendForm.SQUARE // draw legend entries as lines
    legend.formSize = 0.2f
    legend.direction = Legend.LegendDirection.LEFT_TO_RIGHT
}

private fun getSensorLineColor(sensor: Int): Long =
    if(sensor in SENSORS_COLORS.indices) SENSORS_COLORS[sensor]
    else Color.BLACK.toLong()

