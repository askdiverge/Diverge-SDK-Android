package ai.askdiverge.domain.model

import java.util.GregorianCalendar
import java.util.TimeZone

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_DIGITS = 3

private val RFC_3339_DATE_TIME = Regex(
    """(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})(\.\d+)?([Zz]|[+-]\d{2}:\d{2})"""
)

/**
 * Milliseconds since 1970 of an RFC 3339 date-time with a `Z` or `±hh:mm` offset, or `null` when it
 * does not parse. A fraction of any length is cut to its first three digits: history keeps a
 * message's `created_at` as it was stored, often with microseconds, while the livechat log writes
 * milliseconds in UTC, and the two are equal for the same message once both are cut.
 */
internal fun String.toEpochMillisOrNull(): Long? {
    val match = RFC_3339_DATE_TIME.matchEntire(this) ?: return null
    val (year, month, day, hour, minute, second, fraction, offset) = match.destructured
    val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        isLenient = false
        clear()
        set(year.toInt(), month.toInt() - 1, day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
    }
    val utcMillis = try {
        calendar.timeInMillis
    } catch (_: IllegalArgumentException) {
        return null
    }
    val fractionMillis = fraction.removePrefix(".")
        .take(MILLIS_DIGITS)
        .padEnd(MILLIS_DIGITS, '0')
        .toLong()
    return utcMillis + fractionMillis - offsetMillis(offset)
}

private fun offsetMillis(offset: String): Long {
    if (offset.equals("Z", ignoreCase = true)) return 0L
    val sign = if (offset.startsWith("-")) -1L else 1L
    val hours = offset.substring(1, 3).toLong()
    val minutes = offset.substring(4, 6).toLong()
    return sign * (hours * 60 + minutes) * MILLIS_PER_MINUTE
}
