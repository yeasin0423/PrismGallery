package com.example.amoledgallery.util

import com.example.amoledgallery.model.MediaDateSection
import com.example.amoledgallery.model.MediaItem
import com.example.amoledgallery.ui.i18n.LanguageState
import com.example.amoledgallery.ui.i18n.strings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Utility functions for date-based media grouping and section header formatting, in the app's
 * selected UI language.
 */
object DateUtils {

    /**
     * Formats timestamp into Month & Year ("August 2024").
     */
    fun formatMonthYear(timestampSeconds: Long): String {
        val millis = timestampSeconds * 1000L
        val formatted = SimpleDateFormat("MMMM yyyy", LanguageState.locale).format(Date(millis))
        return capitalizeFirst(formatted)
    }

    /**
     * Groups a sorted list of media items into date sections based on grid column count.
     * When columns <= 3 (detail view), items are grouped by day ("Today", "12. September", etc.).
     * When columns >= 4 (compact overview), items are grouped by month ("September 2024").
     */
    fun groupMediaByDate(items: List<MediaItem>, columnCount: Int): List<MediaDateSection> {

        if (items.isEmpty()) return emptyList()

        val groupedMap = LinkedHashMap<String, MutableList<MediaItem>>()
        val titleMap = HashMap<String, String>()

        for (item in items) {
            val key = getGroupKey(item.dateAdded, columnCount)
            if (!groupedMap.containsKey(key)) {
                groupedMap[key] = ArrayList()
                titleMap[key] = formatSectionHeader(item.dateAdded, columnCount)
            }
            groupedMap[key]!!.add(item)
        }

        return groupedMap.map { (key, groupItems) ->
            MediaDateSection(
                title = titleMap[key] ?: key,
                items = groupItems
            )
        }
    }

    /**
     * Generates a unique grouping key for the given date.
     */
    private fun getGroupKey(timestampSeconds: Long, columnCount: Int): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampSeconds * 1000L
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        return if (columnCount >= 4) {
            "M_${year}_$month"
        } else {
            val day = calendar.get(Calendar.DAY_OF_YEAR)
            "D_${year}_$day"
        }
    }

    /**
     * Formats timestamp into a section header text.
     */
    fun formatSectionHeader(timestampSeconds: Long, columnCount: Int): String {
        val itemMillis = timestampSeconds * 1000L
        val itemCalendar = Calendar.getInstance().apply { timeInMillis = itemMillis }
        val nowCalendar = Calendar.getInstance()
        val locale = LanguageState.locale

        // Zoomed out (4-5 columns): Month & Year
        if (columnCount >= 4) {
            return capitalizeFirst(SimpleDateFormat("MMMM yyyy", locale).format(Date(itemMillis)))
        }

        // Zoomed in (2-3 columns): Day format
        if (isSameDay(itemCalendar, nowCalendar)) {
            return strings.today
        }

        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (isSameDay(itemCalendar, yesterdayCalendar)) {
            return strings.yesterday
        }

        val isCurrentYear = itemCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)
        val pattern = if (isCurrentYear) strings.dayPattern else strings.dayWithYearPattern
        return capitalizeFirst(SimpleDateFormat(pattern, locale).format(Date(itemMillis)))
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun capitalizeFirst(str: String): String {
        return str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LanguageState.locale) else it.toString() }
    }
}
