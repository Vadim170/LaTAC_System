package com.kontakt1.tmonitor.utils

import java.util.*
import java.sql.*

object TimeCorrector {
    private var timeCorrection: Long = 0L

    fun readTimeCorrection(connection: Connection) {
        try {
            val stmt = connection.createStatement() ?: return
            val timestamp = Timestamp(0)
            val resultset = stmt.executeQuery("SELECT '$timestamp' AS TIME;")
            resultset.next()
            timeCorrection = resultset.getTimestamp("TIME").time - timestamp.time
            resultset.close()
            stmt.close()
        } finally {

        }
    }

    /**
     *
     * @author Makarov V.G.
     * @param calendarAtClientTimeZone
     * @return
     */
    fun timeInMillisAtServerTimezone(calendarAtClientTimeZone: Calendar): Long {
        return calendarAtClientTimeZone.timeInMillis - timeCorrection
    }

    fun timeInMillisAtClientTimezone(calendarAtServerTimeZone: Calendar): Long {
        return calendarAtServerTimeZone.timeInMillis + timeCorrection
    }

    fun calendarAtServerTimeZone(calendarInClientTimeZone: Calendar) : Calendar {
        val res = Calendar.getInstance()
        res.timeInMillis =
            timeInMillisAtServerTimezone(
                calendarInClientTimeZone
            )
        return res
    }

    fun calendarAtClientTimeZone(calendarAtServerTimeZone: Calendar) : Calendar {
        val res = Calendar.getInstance()
        res.timeInMillis =
            timeInMillisAtClientTimezone(
                calendarAtServerTimeZone
            )
        return res
    }
}