package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.error.ApiErrorEnvelopeRemote
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

internal class FormErrorMapperTest {

    private val envelopeAdapter = Moshi.Builder().build().adapter(ApiErrorEnvelopeRemote::class.java)

    @Test
    fun `a 401 surfaces as session expired`() {
        val error = makeSUT(status = 401, body = "")

        assertIs<ChatbotException.SessionExpired>(error)
    }

    @Test
    fun `a 422 surfaces its field errors, and the server's text stays out of the message`() {
        val body = """
            {
              "error": {
                "code": "validation",
                "message": "jane@example is not a valid email",
                "params": [{ "field": "action.email", "message": "jane@example is not a valid email" }]
              }
            }
        """.trimIndent()

        val error = assertIs<FormRequestException.Validation>(makeSUT(status = 422, body = body))

        assertEquals("Request failed validation", error.message)
        assertEquals(
            listOf(
                FormRequestException.Validation.Param(
                    field = "action.email",
                    message = "jane@example is not a valid email"
                )
            ),
            error.params
        )
    }

    @Test
    fun `any status whose envelope lists params surfaces as validation`() {
        val body = """
            {"error":{"message":"Bad fields","params":[{"field":"name","message":"Required"}]}}
        """.trimIndent()

        val error = assertIs<FormRequestException.Validation>(makeSUT(status = 400, body = body))

        assertEquals(1, error.params.size)
    }

    @Test
    fun `an envelope without a message still surfaces its params`() {
        val body = """{"error":{"params":[{"field":"name","message":"Required"}]}}"""

        val error = assertIs<FormRequestException.Validation>(makeSUT(status = 400, body = body))

        assertEquals(1, error.params.size)
    }

    @Test
    fun `a status without a typed failure keeps the caller's error`() {
        assertNull(makeSUT(status = 500, body = "boom"))
    }

    private fun makeSUT(status: Int, body: String): Throwable? =
        Response.error<String>(status, body.toResponseBody()).mapFormError(envelopeAdapter)
}
