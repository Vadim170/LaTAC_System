package com.kontakt1.tmonitor.ui.utils.chart.markers

import android.content.Context
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.utils.datetime.myStringFormat
import java.util.*

class LvlMarkerView(context: Context?, layoutResource: Int) : MyMarkerView(context,layoutResource) {
    override fun getMarkerText(e: Entry, highlight: Highlight?): String {
        val value = e.y
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = e.x.toLong()
        val isDisplayByServerTimezone =
                (ApplicationData.settingsController?.settingsData?.isDisplayTimeByServerTimeZone == true)
        val date = calendar.myStringFormat(isDisplayByServerTimezone, showSeconds = false, oneLine = true)
        return "$value м \n$date"
    }
}
