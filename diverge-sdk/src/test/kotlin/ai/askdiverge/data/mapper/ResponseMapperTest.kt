package ai.askdiverge.data.mapper

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import retrofit2.Response

internal class ResponseMapperTest {

    @Test
    fun `when the response is successful expect its body`() {
        val result = Response.success("body").mapToResult(errorMessage = "Config error")

        assertEquals(
            expected = "body",
            actual = result.getOrNull()
        )
    }

    @Test
    fun `when a successful response has no body expect a failure with the caller's message`() {
        val result = Response.success<String>(null).mapToResult(errorMessage = "Config error")

        assertTrue(result.isFailure)
        assertEquals(
            expected = "Config error",
            actual = result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `when the response is unsuccessful expect a failure with the caller's message`() {
        val result = Response.error<String>(
            500,
            "boom".toResponseBody()
        ).mapToResult(errorMessage = "Could not get messages")

        assertTrue(result.isFailure)
        assertEquals(
            expected = "Could not get messages",
            actual = result.exceptionOrNull()?.message
        )
    }
}
