package com.kontakt1.tmonitor.ui.utils.datetime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import com.kontakt1.tmonitor.utils.TimeCorrector.calendarAtServerTimeZone
import com.kontakt1.tmonitor.utils.TimeCorrector.timeInMillisAtClientTimezone
import java.util.*

/**
 * Класс для создания форм UI ввода даты и температуры.
 * @author Makarov V.G.
 */
class DateTimePickerDialog(private val context: Context,
                           private var dateTime: Calendar,
                           private val onDateTimeSetListener: OnDateTimeSetListener,
                           defaultDateTime: Calendar,
                           max : Calendar? = null,
                           min : Calendar? = null,
                           private val isWorkingByServerTimeZone: Boolean = false
) {
    private val defaultDateTime: Calendar
    private val max : Calendar?
    private val min : Calendar?

    init {
        if(isWorkingByServerTimeZone) {
            this.defaultDateTime = calendarAtServerTimeZone(defaultDateTime)
            this.max = if(max != null) calendarAtServerTimeZone(max) else null
            this.min = if(min != null) calendarAtServerTimeZone(min) else null
        } else {
            this.defaultDateTime = defaultDateTime
            this.max = max
            this.min = min
        }
    }

    private fun setEditableCalendar(newDateTime: Calendar) {
        if(isWorkingByServerTimeZone)
            dateTime.timeInMillis = timeInMillisAtClientTimezone(newDateTime) // Задаём
        else
            dateTime.timeInMillis = newDateTime.timeInMillis // Задаём
    }

    fun show() {
        DatePickerDialog(
            context, onSetDateFromListener,
            defaultDateTime.get(Calendar.YEAR),
            defaultDateTime.get(Calendar.MONTH),
            defaultDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // установка обработчика выбора даты
    private var onSetDateFromListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
        val listener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            try {
                val newDateTime = Calendar.getInstance()
                newDateTime.set(year, monthOfYear, dayOfMonth, hourOfDay, minute)
                // Этот метод вызывается если и время и дату утвердили. Он применяет изменения к dateTime и вызывает слушателя
                if (this.max != null && newDateTime >= this.max)
                    throw Exception("Неверная дата")
                if (this.min != null && newDateTime <= this.min)
                    throw Exception("Неверная дата")
                setEditableCalendar(newDateTime) // Задаём
                onDateTimeSetListener.onDateTimeSet()
            } catch (e: Exception) {
                onDateTimeSetListener.onException(e)
            }
        }
        TimePickerDialog(
            context, listener,
            this.defaultDateTime.get(Calendar.HOUR_OF_DAY),
            this.defaultDateTime.get(Calendar.MINUTE), true
        ).show()
    }

    interface OnDateTimeSetListener {
        fun onDateTimeSet()
        fun onException(e:Exception)
    }
}