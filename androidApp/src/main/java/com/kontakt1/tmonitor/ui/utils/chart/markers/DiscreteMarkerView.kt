package com.kontakt1.tmonitor.ui.utils.chart.markers

import android.content.Context
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import java.util.*

class DiscreteMarkerView(context: Context?, layoutResource: Int) : MyMarkerView(context,layoutResource) {
    override fun getMarkerText(e: Entry, highlight: Highlight?): String {
        if(highlight?.isStacked != true) return "" // Если выделенные данные на графике не являются коллекцией.
        val indicationList = (e.data as List<*>).map { it as Calendar }
        val isDisplayByServerTimezone =
                (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
        val calendarStr = if(highlight.stackIndex in indicationList.indices && highlight.stackIndex-1 in indicationList.indices) {
            "${indicationList[highlight.stackIndex-1].myStringFormat(isDisplayByServerTimezone, oneLine = true)}\n" +
            " - ${indicationList[highlight.stackIndex].myStringFormat(isDisplayByServerTimezone, oneLine = true)}"
        } else ""
        val position = when(highlight.dataSetIndex) {
            0 -> "↓"
            1 -> "↑"
            else -> "?"
        }
        return "$position $calendarStr"
    }
}