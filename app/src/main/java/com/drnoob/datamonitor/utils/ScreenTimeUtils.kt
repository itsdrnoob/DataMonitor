package com.drnoob.datamonitor.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Typeface
import android.net.ParseException
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel
import kotlin.math.roundToInt

fun loadScreenTime(
    context: Context,
    model: AppDataUsageModel,
    appScreenTime: TextView,
    appBackgroundTime: TextView
) {
    if (model.packageName !== context.getString(R.string.package_tethering)) {
        val usageTime = getUsageTime(context, model.packageName, model.session)
        if (usageTime[1] == -1) {
            // If value is -1, build version is below Q
            val screenTime: String = context.getString(
                R.string.app_label_screen_time,
                formatTime(context, usageTime[0] / 60f)
            )
            appScreenTime.text = setBoldSpan(screenTime, formatTime(context, usageTime[0] / 60f))
            appBackgroundTime.visibility = View.GONE
        } else {
            val screenTime: String = context.getString(
                R.string.app_label_screen_time,
                formatTime(context, usageTime[0] / 60f)
            )
            val backgroundTime: String = context.getString(
                R.string.app_label_background_time,
                formatTime(context, usageTime[0] / 60f)
            )
            appScreenTime.text = setBoldSpan(screenTime, formatTime(context, usageTime[0] / 60f))
            appBackgroundTime.text =
                setBoldSpan(backgroundTime, formatTime(context, usageTime[0] / 60f))
        }
    } else {
        appScreenTime.visibility = View.GONE
        appBackgroundTime.visibility = View.GONE
    }
}

fun setBoldSpan(textString: String, spanText: String): SpannableString {
    val boldSpan = SpannableString(textString)
    val start = textString.indexOf(spanText).takeIf { it >= 0 } ?: 0
    val end = start + spanText.length
    boldSpan.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    return boldSpan
}

/**
 * Returns app usage time as an array like [screenTime, backgroundTime]
 * ScreenTime source credit: https://stackoverflow.com/questions/61677505/how-to-count-app-usage-time-while-app-is-on-foreground
 */
private fun getUsageTime(context: Context, packageName: String, session: Int): IntArray {

    val allEvents: MutableList<UsageEvents.Event> = mutableListOf()
    val appScreenTime = HashMap<String, Int>().apply { put(packageName, 0) }
    val appBackgroundTime = HashMap<String, Int?>().apply { put(packageName, -1) }
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val timePeriod = NetworkStatsHelper.getTimePeriod(context, session, 1)

    try {
        val usageEvents = usageStatsManager.queryEvents(timePeriod[0], timePeriod[1])
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            timePeriod[0], timePeriod[1]
        )

        while (usageEvents.hasNextEvent()) {
            val currentEvent = UsageEvents.Event()
            usageEvents.getNextEvent(currentEvent)
            if (currentEvent.packageName == packageName && currentEvent.eventType in setOf(
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.FOREGROUND_SERVICE_START,
                    UsageEvents.Event.FOREGROUND_SERVICE_STOP
                )
            ) {
                allEvents.add(currentEvent)
            }
        }

        if (allEvents.size > 0) {
            allEvents.zipWithNext { event, nextEvent ->
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                    nextEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED &&
                    event.className == nextEvent.className
                ) {
                    val diff = (nextEvent.timeStamp - event.timeStamp).toInt() / 1000
                    val prev = appScreenTime[event.packageName] ?: 0
                    appScreenTime[event.packageName] = prev + diff
                }
            }
            val lastEvent = allEvents.last()
            if (lastEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val diff = (System.currentTimeMillis() - lastEvent.timeStamp) / 1000
                val prev = appScreenTime[lastEvent.packageName] ?: 0
                appScreenTime[lastEvent.packageName] = prev + diff.toInt()
            }
        } else {
            appScreenTime[packageName] = 0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!usageStats.isNullOrEmpty()) {
                for (stats in usageStats) {
                    if (stats.packageName == packageName) {
                        val backgroundTime = stats.totalTimeForegroundServiceUsed.toInt() / 1000
                        appBackgroundTime[packageName] = backgroundTime
                        break
                    }
                }
            } else {
                appBackgroundTime[packageName] = 0
            }
        } else {
            appBackgroundTime[packageName] = 0
        }
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return intArrayOf(appScreenTime[packageName]!!, appBackgroundTime[packageName]!!)
}

private fun formatTime(context: Context, minutes: Float): String {

    if (minutes < 1 && minutes > 0) return "Less than a minute"
    if (minutes >= 60) {
        val hours = (minutes / 60).toInt()
        val mins = (minutes % 60).toInt()
        val hourLabel: String = if (hours > 1) "hours" else "hour"
        val minuteLabel: String = if (mins == 1) "minute" else "minutes"
        return context.getString(R.string.usage_time_label, hours, hourLabel, mins, minuteLabel)
    }
    return if (minutes == 1f) {
        minutes.roundToInt().toString() + " minute"
    } else minutes.roundToInt().toString() + " minutes"
}
