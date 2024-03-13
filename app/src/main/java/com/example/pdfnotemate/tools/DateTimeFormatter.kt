package com.example.pdfnotemate.tools

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**FORMATS CATALOGUE (Sun Jan 01 22:15:30 GMT+05:30 2012)
 * You can add custom date format using below formats

E -> Sun

EE -> Sun

EEE -> Sun

EEEE -> Sunday

.

M -> 1 (month in number)

MM -> 01 (month in number)

MMM -> Jan

MMMM -> January

MMMMM -> J

.

w -> 1 (week)

ww -> 01 (week)

W -> 1 (week)

WW -> 01 (week)

.

y -> 2012

yy -> 12

yyy -> 2012

yyyyy -> 2012

.

H -> 22 (hour in 24 hour format)

HH -> 22 (hour in 24 hour format)

h -> 10 (hour in 12 hour format)

hh -> 10 (hour in 12 hour format)

.

m -> 15 (minute)

mm -> 15 (minute)

.

s -> 30 (second)

ss -> 30 (second)

.

a -> PM (AM/PM)

.

z -> GMT+05:30 (GMT Zone)

Z -> +0530 (GMT Zone)*/
@SuppressLint("SimpleDateFormat")
object DateTimeFormatter {
    private const val TAG = "DateTimeFormatter"

    fun format(
        inputDate: String,
        fromFormat: String,
        toFormat: String,
        fromTimeZone: TimeZone = TimeZone.getDefault(),
        toTimeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.ENGLISH,
    ): String {
        return try {
            val sdfFrom = SimpleDateFormat(fromFormat)
            sdfFrom.timeZone = fromTimeZone
            val date = sdfFrom.parse(inputDate)
            val sdfTo = SimpleDateFormat(toFormat, locale)
            sdfTo.timeZone = toTimeZone
            return sdfTo.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "failed to format date $inputDate from $fromFormat to $toFormat")
            inputDate
        }
    }

    fun format(
        inputDate: Date,
        toFormat: String,
        toTimeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.ENGLISH,
    ): String {
        return try {
            val sdfTo = SimpleDateFormat(toFormat, locale)
            sdfTo.timeZone = toTimeZone
            return sdfTo.format(inputDate)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "failed to format date $inputDate. to $toFormat")
            inputDate.toGMTString()
        }
    }
    fun format(
        timeInMilli: Long,
        toFormat: String,
        toTimeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.ENGLISH,
    ): String {
        val inputDate = dateFromMilliseconds(timeInMilli)
        return try {
            val sdfTo = SimpleDateFormat(toFormat, locale)
            sdfTo.timeZone = toTimeZone
            return sdfTo.format(inputDate)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "failed to format date $inputDate. to $toFormat")
            inputDate.toString()
        }
    }

    /**
     * @return returns the date object corresponding to given milliseconds
     * */
    fun dateFromMilliseconds(
        inputMilliSeconds: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Date {
        return try {
            val calender = Calendar.getInstance()
            calender.timeInMillis = inputMilliSeconds
            calender.timeZone = timeZone
            calender.time
        } catch (e: Exception) {
            Calendar.getInstance().time
        }
    }

    fun dateFromDateString(
        inputDate: String,
        fromFormat: String,
    ): Date? {
        return try {
            val sdfFrom = SimpleDateFormat(fromFormat)
            sdfFrom.parse(inputDate)
        } catch (e: Exception) {
            null
        }
    }

    fun convertMilliToMinSec(timeInMilli: Long): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timeInMilli) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeInMilli)),
            TimeUnit.MILLISECONDS.toSeconds(timeInMilli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMilli)),
        )
    }

    fun convertMilliToHourMinSec(timeInMilli: Long): String {
        val hour = TimeUnit.MILLISECONDS.toHours(timeInMilli)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilli) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeInMilli))
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMilli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMilli))
        if (hour > 0) {
            return String.format("%02d:%02d:%02d", hour, minutes, seconds)
        }

        return String.format("%02d:%02d", minutes, seconds)
    }

    fun convertTwentyFourToAmPm(time: String): String {
        return format(time, TwentyFourFormat, AmPmFormat)
    }

    fun getGmtTimeZone(): TimeZone {
        return TimeZone.getTimeZone("Etc/UTC")
    }

    private const val TIME_SERVER_URL = "https://worldtimeapi.org/api/timezone/Etc/UTC"

    /**try to get server time, if it fails then this function will return system time*/
    suspend fun getGmtTimeStamp(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(TIME_SERVER_URL)
                val connection = url.openConnection() as HttpURLConnection
//              connection.requestMethod = "GET"
                val inputStreamReader = InputStreamReader(connection.inputStream)
                val bufferReader = BufferedReader(inputStreamReader)
                val stringBuilder = java.lang.StringBuilder()
                var line: String?
                while (bufferReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                bufferReader.close()
                inputStreamReader.close()
                connection.disconnect()
                val jsonObj = JSONObject(stringBuilder.toString())
                val timeInMillis = jsonObj.get("unixtime").toString().toLongOrNull() ?: throw Exception("failed to get time")
                timeInMillis * 1000
            } catch (e: Exception) {
                e.printStackTrace()
                System.currentTimeMillis()
            }
        }
    }

    /**dd-MM-yy => 31-01-12*/
    const val DD_MM_YY = "dd-MM-yy"

    /**dd-MM-yyyy => 31-01-2012*/
    const val DD_MM_YYYY = "dd-MM-yyyy"

    /**MM-dd-yyyy => 01-31-2012*/
    const val MM_DD_YYYY = "MM-dd-yyyy"

    /**MM-dd-yyyy => 31 jan 2012*/
    const val DD_MMM_YYYY = "dd MMM yyyy"

    /**14:23:24*/
    const val TwentyFourFormat = "HH:mm:ss"

    /**14:23*/
    const val TwentyFourFormatNoSeconds = "HH:mm"

    const val AmPmFormat = "hh:mm a"

    const val DayName = "EEEE"

    /**2023-01-11T09:06:18.028527Z*/
    const val TIME_AND_DATE_GMT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /**10 July, 10:35 AM*/
    const val DATE_TIME_FORMAT_ONE = "dd MMMM, hh:mm a"

    /**10 July 2022, 10:35 AM*/
    const val DATE_TIME_FORMAT_TWO = "dd MMMM yyyy, hh:mm a"

    /**10 Jan 2022, 15:28:23*/
    const val D_M_Y_h_m_s = "dd MMM yyyy, HH:mm:ss"

    /**10 Jan 2022, 3:28:23 */
    const val D_M_Y_h_m_s_12 = "dd MMM yyyy, hh:mm:ss a"

    /**Wednesday,  Jan 14 - 2023*/
    const val DAY_MONTH_YEAR_FORMAT = "EEEE, MMM dd - yyy"

    /**2023-11-19T15:11:51*/
    const val DATE_AND_TIME_THREE = "HH:mm:ss yyyy-MM-dd'T'"
}
