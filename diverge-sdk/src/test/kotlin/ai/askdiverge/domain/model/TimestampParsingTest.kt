package ai.askdiverge.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TimestampParsingTest {

    @Test
    fun `a UTC timestamp without a fraction is read to the second`() {
        assertEquals(1_700_000_000_000L, "2023-11-14T22:13:20Z".toEpochMillisOrNull())
        assertEquals(1_700_000_000_000L, "2023-11-14t22:13:20z".toEpochMillisOrNull())
    }

    @Test
    fun `an offset is taken off the local time`() {
        assertEquals(1_700_000_000_000L, "2023-11-15T00:13:20+02:00".toEpochMillisOrNull())
        assertEquals(1_700_000_000_000L, "2023-11-14T16:43:20-05:30".toEpochMillisOrNull())
        assertEquals(1_700_000_000_000L, "2023-11-14T22:13:20+00:00".toEpochMillisOrNull())
    }

    @Test
    fun `a fraction is cut to milliseconds whatever its length`() {
        assertEquals(1_700_000_000_500L, "2023-11-14T22:13:20.5Z".toEpochMillisOrNull())
        assertEquals(1_700_000_000_123L, "2023-11-14T22:13:20.123Z".toEpochMillisOrNull())
        assertEquals(1_700_000_000_123L, "2023-11-14T22:13:20.123999Z".toEpochMillisOrNull())
    }

    @Test
    fun `a history timestamp with microseconds equals the livechat log's milliseconds for the same instant`() {
        assertEquals(
            "2026-01-01T10:00:00.123Z".toEpochMillisOrNull(),
            "2026-01-01T10:00:00.123456+00:00".toEpochMillisOrNull()
        )
        assertEquals(
            "2026-01-01T10:00:00.123Z".toEpochMillisOrNull(),
            "2026-01-01T11:00:00.123999+01:00".toEpochMillisOrNull()
        )
    }

    @Test
    fun `one millisecond apart stays one millisecond apart`() {
        val earlier = "2026-01-01T10:00:00.123456+00:00".toEpochMillisOrNull()
        val later = "2026-01-01T10:00:00.124Z".toEpochMillisOrNull()

        assertEquals(1L, requireNotNull(later) - requireNotNull(earlier))
    }

    @Test
    fun `text that is not an RFC 3339 date-time has no time`() {
        assertNull("".toEpochMillisOrNull())
        assertNull("not-a-date".toEpochMillisOrNull())
        assertNull("2023-11-14".toEpochMillisOrNull())
        assertNull("2023-11-14 22:13:20Z".toEpochMillisOrNull())
        assertNull("2023-11-14T22:13:20".toEpochMillisOrNull())
        assertNull("2023-11-14T22:13:20+0200".toEpochMillisOrNull())
        assertNull(" 2023-11-14T22:13:20Z".toEpochMillisOrNull())
    }

    @Test
    fun `a date or time that does not exist has no time`() {
        assertNull("2023-02-30T00:00:00Z".toEpochMillisOrNull())
        assertNull("2023-13-01T00:00:00Z".toEpochMillisOrNull())
        assertNull("2023-11-14T24:00:00Z".toEpochMillisOrNull())
    }
}
