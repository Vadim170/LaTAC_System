package com.kontakt1.tmonitor.ui.utils.chart

import android.graphics.Typeface
import android.view.View
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.DiagramFragment.Companion.MAX_T_Y
import com.kontakt1.tmonitor.ui.activities.mainActivityFragments.DiagramFragment.Companion.MIN_T_Y
import com.kontakt1.tmonitor.dataClasses.indications.DiscreteIndication
import com.kontakt1.tmonitor.dataClasses.indications.TIndication
import com.kontakt1.tmonitor.dataClasses.indications.interfaces.Indication
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.AnalogParams
import com.kontakt1.tmonitor.dataClasses.params.interfaces.DiscreteParams
import com.kontakt1.tmonitor.dataClasses.params.interfaces.Param
import com.kontakt1.tmonitor.ui.utils.chart.markers.DiscreteMarkerView
import java.util.*


/**
 * Регулировка внешнего вида графика, отрисовка полос уставок.
 */
fun HorizontalBarChart.adjustTempChart() {
    val selectedParam = ApplicationData.system.selectedParam
    configDiagramDesign(this)
    if(selectedParam != null) {
        // Если параметр аналоговый(Значит имеет уставки)
        if((selectedParam as? AnalogParams<*>) != null) {
            setConstraintLines(
                this.axisLeft,
                selectedParam.constraintsList,
                false
            ) // Настройка линий уставок
        }
    }
}

fun HorizontalBarChart.adjustDiscreteChart() {
    val selectedParam = ApplicationData.system.selectedParam
    configDiscreteDiagramDesign(
            this,
            selectedParam
        )
}

fun HorizontalBarChart.mySetDiscreteData(
    param: Param<*>, indications: List<Indication>?, dateTimeFrom: Calendar, dateTimeTo: Calendar
) {
    configLeftAxis(dateTimeFrom, dateTimeTo)
    if(indications == null) return
    val displayIndications =
        addFirstAdnLastInications(
            dateTimeFrom,
            indications,
            dateTimeTo
        )
    val values = getSequentialListIntervals(
        displayIndications
    )
    val colors = getSequentialListColors(
        displayIndications,
        values
    )
    val position = when(param) {
        is LDUpParam -> 1
        is LDDownParam -> 0
        else -> 0
    }
    val barDataSet = getDataSet(
        values,
        position,
        colors,
        displayIndications
    )
    if(data?.dataSets != null) {
        val dataSets = data.dataSets.toMutableList()
        if(dataSets.size <= position) dataSets.add(barDataSet)
        else dataSets[position] = barDataSet
        data = BarData(dataSets)
    }
    else
        data = BarData(listOf(barDataSet))

}

fun addFirstAdnLastInications(
    dateTimeFrom: Calendar,
    indications: List<Indication>,
    dateTimeTo: Calendar
): List<Indication> {
    val res: MutableList<Indication> = mutableListOf(
        DiscreteIndication(dateTimeFrom,DiscreteIndication.DiscreteSensorState.UNKNOWN))
    res.addAll(indications)
    res.add(DiscreteIndication(dateTimeTo,DiscreteIndication.DiscreteSensorState.UNKNOWN))
    return res
}

private fun getDataSet(
    values: List<Float>,
    position: Int,
    colors: List<Int>,
    indications: List<Indication>
): BarDataSet {
    val valuesArray = values.toFloatArray()
    val barEntrys =
        if (values.isEmpty()) listOf<BarEntry>()
        else listOf(BarEntry(
            position.toFloat(),
            valuesArray,
            indications.map { it.dateTime }
        ))
    val barDataSet = BarDataSet(barEntrys, "")
    barDataSet.colors = colors
    barDataSet.setDrawValues(false)
    barDataSet.barBorderWidth = 0.1f
    return barDataSet
}

private fun getSequentialListColors(indications: List<Indication>, values: List<Float>): List<Int> {
    val valuesIterator = values.iterator()
    val colors =
        indications.map {
            if(valuesIterator.hasNext()) {
                if(valuesIterator.next() >= DiscreteParams.NUMBER_MINUTES_RELEVANT*60_000)
                    DiscreteIndication.DiscreteSensorState.UNKNOWN.getColor()
                else
                    (it as DiscreteIndication).value.getColor()
            }
            else
                (it as DiscreteIndication).value.getColor()
        }.toMutableList()
    colors.add(DiscreteIndication.DiscreteSensorState.UNKNOWN.getColor())
    return colors
}

private fun getSequentialListIntervals(indications: List<Indication>): List<Float> {
    var prevDT = indications.first().dateTime.timeInMillis.toFloat()
    return indications.mapIndexed { index, indication ->
        indication as DiscreteIndication
        val res = indication.dateTime.timeInMillis.toFloat() - prevDT
        prevDT = indication.dateTime.timeInMillis.toFloat()
        res
    }
}

private fun HorizontalBarChart.configLeftAxis(
    dateTimeFrom: Calendar,
    dateTimeTo: Calendar
) {
    axisLeft.axisMinimum = 0f
    axisLeft.axisMaximum = dateTimeTo.timeInMillis.toFloat() - dateTimeFrom.timeInMillis.toFloat()
}

fun HorizontalBarChart.mySetTempData(indication: Indication) {
    indication as TIndication
    val spaceForBar = 1f
    val values = indication.temp.mapIndexed { index, value->
        BarEntry((index+1) * spaceForBar, value ?: 0f)
    }
    val set = BarDataSet(values, "Температуры")
    set.color = 0xFF00D000.toInt()
    set.setDrawIcons(false)
    val dataSets= listOf(set)
    val data = BarData(dataSets)
    data.setValueTextSize(10f)
    data.setValueTypeface(Typeface.SANS_SERIF)
    data.barWidth = 0.9f
    this.xAxis.labelCount = values.count()
    this.data = data
}

fun configDiagramDesign(diagram: HorizontalBarChart) {
    diagram.setDrawBarShadow(false)
    diagram.setDrawValueAboveBar(true)
    diagram.description.isEnabled = false
    // if more than 60 entries are displayed in the chart, no values will be drawn
    diagram.setMaxVisibleValueCount(60)
    // scaling can now only be done on x- and y-axis separately
    diagram.setPinchZoom(false)
    diagram.isDragEnabled = false // enable scaling and dragging
    diagram.isScaleXEnabled = false
    diagram.isScaleYEnabled = false
    diagram.setDrawGridBackground(false)
    diagram.xAxis.let { xl ->
        xl.position = XAxis.XAxisPosition.BOTTOM
        xl.typeface = Typeface.SANS_SERIF
        xl.setDrawAxisLine(true)
        xl.setDrawGridLines(false)
        xl.granularity = 1f
    }
    diagram.axisLeft.let { yl -> // Верхняя ось
         yl.typeface = Typeface.SANS_SERIF
        yl.setDrawAxisLine(true)
        yl.setDrawGridLines(true)
        yl.axisMinimum = MIN_T_Y // this replaces setStartAtZero(true)
        yl.axisMaximum = MAX_T_Y
    }
    diagram.axisRight.let { yr -> // Нижняя ось
        yr.typeface = Typeface.SANS_SERIF
        yr.setDrawAxisLine(true)
        yr.setDrawGridLines(false)
        yr.axisMinimum = MIN_T_Y// this replaces setStartAtZero(true)
        yr.axisMaximum = MAX_T_Y
    }
    diagram.setFitBars(true)
    diagram.animateY(500)
}

fun configDiscreteDiagramDesign(diagram: HorizontalBarChart, selectedParam: Param<*>?) {
    //diagram.setDrawBorders(true)
    diagram.description.isEnabled = false
    diagram.setPinchZoom(false)
    diagram.isDragEnabled = false // true // enable scaling and dragging
    diagram.isScaleXEnabled = false //true
    diagram.isScaleYEnabled = false
    diagram.extraLeftOffset = 15f // Сдвинем грануцу самого левого лейбла
    diagram.extraRightOffset = 15f // Сдвинем грануцу самого правого лейбла
    diagram.extraTopOffset = 0f
    diagram.extraBottomOffset = 0f
    diagram.xAxis.let { xl ->
        xl.position = XAxis.XAxisPosition.BOTTOM
        xl.typeface = Typeface.DEFAULT_BOLD
        xl.textSize = 20.0F
        xl.setDrawAxisLine(false)
        xl.setDrawGridLines(false)
        xl.granularity = 1f
        xl.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when(value) {
                    0f -> "↓"
                    1f -> "↑"
                    else -> ""
                }
            }
        }
        xl.axisMaximum = 1.5f
        xl.axisMinimum = -0.5f
    }
    diagram.axisLeft.setDrawLabels(false)
    diagram.axisLeft.setDrawAxisLine(false)
    diagram.axisLeft.setDrawGridLines(false)
    diagram.axisRight.isEnabled = false
    diagram.setFitBars(true)
    diagram.legend.isEnabled = false
    diagram.setDrawMarkers(true)
    diagram.marker = when (selectedParam) {
        is LParam -> DiscreteMarkerView(
            diagram.context,
            R.layout.custom_marker_view
        )
        is LDDownParam -> DiscreteMarkerView(
            diagram.context,
            R.layout.custom_marker_view
        )
        else -> null
    }
}