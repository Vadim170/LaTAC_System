package com.kontakt1.tmonitor.utils.datetime

import com.kontakt1.tmonitor.utils.TimeCorrector
//import com.sun.org.apache.xpath.internal.operations.Bool
import java.text.SimpleDateFormat
import java.util.*

/**
 * Формирует строку содержащую дату и время.
 * если float не задан
 */
fun Calendar.myStringFormat(isDisplayTimeByServerTimeZone: Boolean, showSeconds: Boolean = false, oneLine:Boolean = false) : String{
	val dateFormat =
			getMySimpleDateFormat(
					showSeconds,
					oneLine
			)
	val dateTimeForDisplay =
			getCalendarForDisplay(this, isDisplayTimeByServerTimeZone)
	return dateFormat.format(dateTimeForDisplay.time)
}

fun getCalendarForDisplay(calendar: Calendar, isDisplayTimeByServerTimeZone: Boolean): Calendar {
	return if(isDisplayTimeByServerTimeZone) {
		TimeCorrector.calendarAtServerTimeZone(
			calendar
		)
	} else {
		calendar
	}
}

private fun getMySimpleDateFormat(showSeconds: Boolean = false, oneLine:Boolean = false) : SimpleDateFormat {
	val splitter = if(oneLine) " " else "\n"
	return if(showSeconds)
		SimpleDateFormat("H:mm:ss${splitter}d.M.yy", Locale.US)
	else
		SimpleDateFormat("H:mm${splitter}d.M.yy", Locale.US)
}

fun Calendar.myRestDateTimeFormat(showSeconds: Boolean = false, oneLine:Boolean = false) : String{
	val isDisplayTimeByServerTimeZone = false
	val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
	val dateTimeForDisplay = getCalendarForDisplay(this, isDisplayTimeByServerTimeZone)
	return dateFormat.format(dateTimeForDisplay.time)
}