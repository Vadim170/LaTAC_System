package com.kontakt1.tmonitor.asktweb.api.v1

import java.text.SimpleDateFormat
import java.util.*

class DateTimeParser {
    companion object {
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH)

        fun dateTimeParse(dateTimeFromString: String): Calendar {
            val dateTimeFrom = Calendar.getInstance()
            dateTimeFrom.time = simpleDateFormat.parse(dateTimeFromString)
            return dateTimeFrom
        }
    }
}