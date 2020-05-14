package com.kontakt1.tmonitor.ui.utils.chart.markers

import android.content.Context
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.custom_marker_view.view.*


abstract class MyMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        val text = getMarkerText(e, highlight)
        tvContent.text = text
        super.refreshContent(e, highlight)
    }

    protected abstract fun getMarkerText(e: Entry, highlight: Highlight?): String

    override fun getOffset() =
        MPPointF((-(width / 2)).toFloat(), (-height).toFloat())


}
