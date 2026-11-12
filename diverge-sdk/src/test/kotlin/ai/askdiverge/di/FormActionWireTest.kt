package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.mapper.mapToRemote
import ai.askdiverge.data.model.form.ChatFormValuesPatchRequestRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * What leaves the SDK on `POST /actions` and `GET /forms/{id}`. No server is involved: a terminal
 * interceptor records each request and answers it in-process.
 */
internal class FormActionWireTest {

    private val formDefinitionBody = """
        {
          "form_id": "livechatWaitingContact",
          "name": "Waiting contact",
          "fields": [
            { "key": "email", "label": "Email", "type": "email" },
            { "key": "order_number", "label": "Order", "type": "text" }
          ],
          "min_filled_fields": 1
        }
    """.trimIndent()

    @Nested
    @DisplayName("POST /actions")
    inner class SubmitActionCalls {

        @Test
        fun `a camelCase field key survives verbatim on the wire`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"submission_id":"sub_1","confirmation_text":"Thanks"}"""
                )
            )

            val result = sut.submitAction(
                SubmitActionRequest(
                    partId = "part_contact_1",
                    currentPage = null,
                    action = SubmitAction.ContactForm(
                        fields = mapOf(
                            "orderNumber" to "ABC-123",
                            "email" to "jane@example.com"
                        )
                    )
                ).mapToRemote()
            )

            assertTrue(result.isSuccess)
            assertEquals("sub_1", result.getOrNull()?.submission_id)
            assertEquals("Thanks", result.getOrNull()?.confirmation_text)

            val request = recorder.requests.single()
            assertEquals("POST", request.method)
            assertTrue(request.url.encodedPath.endsWith("/v1/chat/actions"))
            assertEquals("Bearer token-1", request.header("Authorization"))

            assertEquals(
                """{"part_id":"part_contact_1","action":{"type":"contact_form",""" +
                    """"fields":{"orderNumber":"ABC-123","email":"jane@example.com"}}}""",
                recorder.bodies.single()
            )
        }

        @Test
        fun `support_ticket encodes the page, name email message and attachment snake_case fields`() = runTest {
            val photo = requireNotNull(
                OutgoingAttachment.create(
                    data = "QQ==",
                    mime = "image/jpeg",
                    filename = "photo.jpg",
                    maxDecodedBytes = OutgoingAttachment.MAX_ACTION_DECODED_BYTES
                )
            )
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"submission_id":"sub_2"}"""
                )
            )

            sut.submitAction(
                SubmitActionRequest(
                    partId = "part_ticket_1",
                    currentPage = "https://shop.example.com/products/42",
                    action = SubmitAction.SupportTicket(
                        name = "Jane",
                        email = "jane@example.com",
                        message = "Broken",
                        attachments = listOf(photo)
                    )
                ).mapToRemote()
            )

            assertEquals(
                """{"part_id":"part_ticket_1","current_page_url":"https://shop.example.com/products/42",""" +
                    """"action":{"type":"support_ticket","name":"Jane",""" +
                    """"email":"jane@example.com","message":"Broken","attachments":[""" +
                    """{"filename":"photo.jpg","mime_type":"image/jpeg","data_base64":"QQ=="}]}}""",
                recorder.bodies.single()
            )
        }

        @Test
        fun `form_submission encodes values and files keyed by FormField key`() = runTest {
            val photo = requireNotNull(
                OutgoingAttachment.create(
                    data = "QQ==",
                    mime = "image/jpeg",
                    filename = "photo.jpg",
                    maxDecodedBytes = OutgoingAttachment.MAX_ACTION_DECODED_BYTES
                )
            )
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"submission_id":"sub_3"}"""
                )
            )

            sut.submitAction(
                SubmitActionRequest(
                    partId = "part_form_1",
                    currentPage = null,
                    action = SubmitAction.FormSubmission(
                        formId = "salesLead",
                        values = mapOf("companyName" to "Acme"),
                        files = mapOf("photo" to photo)
                    )
                ).mapToRemote()
            )

            assertEquals(
                """{"part_id":"part_form_1","action":{"type":"form_submission","form_id":"salesLead",""" +
                    """"values":{"companyName":"Acme"},"files":{"photo":""" +
                    """{"filename":"photo.jpg","mime_type":"image/jpeg","data_base64":"QQ=="}}}}""",
                recorder.bodies.single()
            )
        }

        @Test
        fun `a 401 on actions surfaces as sessionExpired without a retry`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(status = 401, body = ""),
                ScriptedResponse(status = 200, body = """{"submission_id":"sub_1"}""")
            )

            val result = sut.submitAction(
                SubmitActionRequest(
                    partId = "part_contact_1",
                    currentPage = null,
                    action = SubmitAction.ContactForm(fields = mapOf("email" to "a@b.c"))
                ).mapToRemote()
            )

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
            assertEquals(1, recorder.requests.size)
        }

        @Test
        fun `a 409 on actions surfaces as conflict`() = runTest {
            val (sut, _) = makeSUT(
                ScriptedResponse(
                    status = 409,
                    body = """{"error":{"code":"conflict","message":"Form already submitted"}}"""
                )
            )

            val result = sut.submitAction(
                SubmitActionRequest(
                    partId = "part_contact_1",
                    currentPage = null,
                    action = SubmitAction.ContactForm(fields = mapOf("email" to "a@b.c"))
                ).mapToRemote()
            )

            assertIs<FormRequestException.Conflict>(result.exceptionOrNull())
        }

        @Test
        fun `confirmation_text null and missing both decode to null`() = runTest {
            val (nullSut, _) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"submission_id":"s","confirmation_text":null}"""
                )
            )
            val (missingSut, _) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"submission_id":"s"}"""
                )
            )
            val request = SubmitActionRequest(
                partId = "p",
                currentPage = null,
                action = SubmitAction.ContactForm(fields = mapOf("email" to "a@b.c"))
            ).mapToRemote()

            assertNull(nullSut.submitAction(request).getOrNull()?.confirmation_text)
            assertNull(missingSut.submitAction(request).getOrNull()?.confirmation_text)
        }
    }

    @Nested
    @DisplayName("GET /forms/{id}")
    inner class FetchForm {

        @Test
        fun `GET forms id sends bearer and decodes the definition`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = formDefinitionBody
                )
            )

            val result = sut.fetchForm("livechatWaitingContact")

            assertTrue(result.isSuccess)
            assertEquals("livechatWaitingContact", result.getOrNull()?.form_id)
            assertEquals(2, result.getOrNull()?.fields?.size)
            assertEquals(1, result.getOrNull()?.min_filled_fields)

            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/v1/chat/forms/livechatWaitingContact"))
            assertEquals("Bearer token-1", request.header("Authorization"))
        }

        @Test
        fun `a form id is sent as a single path segment`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = formDefinitionBody
                )
            )

            sut.fetchForm("a/../b")

            assertEquals(
                "/api/v1/chat/forms/a%2F..%2Fb",
                recorder.requests.single().url.encodedPath
            )
        }

        @Test
        fun `a 401 on GET forms surfaces as session expiry without a retry`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(status = 401, body = ""),
                ScriptedResponse(status = 200, body = formDefinitionBody)
            )

            val result = sut.fetchForm("livechatWaitingContact")

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
            assertEquals(1, recorder.requests.size)
        }

        @Test
        fun `a 404 on GET forms surfaces as notFound`() = runTest {
            val (sut, _) = makeSUT(
                ScriptedResponse(
                    status = 404,
                    body = """{"error":{"code":"not_found","message":"Form not found"}}"""
                )
            )

            val result = sut.fetchForm("gone")

            assertIs<FormRequestException.NotFound>(result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("GET /session and PATCH /forms/{id}/values")
    inner class SessionFormCalls {

        private val sessionBody = """
            {
              "values": [
                {
                  "key": "email",
                  "label": "Email",
                  "type": "email",
                  "value": "a@b.c",
                  "updated_at": "2026-01-01T00:00:00Z"
                }
              ]
            }
        """.trimIndent()

        @Test
        fun `GET session sends bearer and decodes values`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = sessionBody))

            val result = sut.getSession()

            assertTrue(result.isSuccess)
            assertEquals("email", result.getOrNull()?.values?.single()?.key)
            assertEquals("a@b.c", result.getOrNull()?.values?.single()?.value)
            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/v1/chat/session"))
            assertEquals("Bearer token-1", request.header("Authorization"))
        }

        @Test
        fun `PATCH values pins the body and form id path`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = sessionBody))

            val result = sut.patchFormValues(
                formId = "livechatWaitingContact",
                body = ChatFormValuesPatchRequestRemote(values = mapOf("email" to "a@b.c"))
            )

            assertTrue(result.isSuccess)
            val request = recorder.requests.single()
            assertEquals("PATCH", request.method)
            assertTrue(
                request.url.encodedPath.endsWith("/v1/chat/forms/livechatWaitingContact/values")
            )
            assertTrue(recorder.bodies.single().contains("\"email\":\"a@b.c\""))
        }

        @Test
        fun `a 409 on PATCH values surfaces as conflict`() = runTest {
            val (sut, _) = makeSUT(ScriptedResponse(status = 409, body = ""))

            val result = sut.patchFormValues(
                formId = "livechatWaitingContact",
                body = ChatFormValuesPatchRequestRemote(values = mapOf("email" to "a@b.c"))
            )

            assertIs<FormRequestException.Conflict>(result.exceptionOrNull())
        }

        @Test
        fun `a 422 on PATCH values surfaces validation for values paths`() = runTest {
            val (sut, _) = makeSUT(
                ScriptedResponse(
                    status = 422,
                    body = """
                        {
                          "error": {
                            "code": "validation_error",
                            "message": "Invalid",
                            "params": [{ "field": "values.email", "message": "bad" }]
                          }
                        }
                    """.trimIndent()
                )
            )

            val result = sut.patchFormValues(
                formId = "livechatWaitingContact",
                body = ChatFormValuesPatchRequestRemote(values = mapOf("email" to "nope"))
            )

            val error = assertIs<FormRequestException.Validation>(result.exceptionOrNull())
            assertEquals("values.email", error.params.single().field)
        }
    }

    private fun makeSUT(vararg responses: ScriptedResponse): Pair<ChatbotRemoteDataSource, ScriptedInterceptor> {
        val recorder = ScriptedInterceptor(responses.toList())
        val okHttp = buildOkHttpClient(
            tokenLocalDataSource = TokenLocalDataSourceImpl().apply { setToken("token-1") },
            sdkVersion = "9.8.7",
            clientProfiles = listOf(ClientProfile.PRODUCT_RECOMMENDATION)
        ).newBuilder()
            .addInterceptor(recorder)
            .build()

        return createRemoteDataSource(
            environment = DivergeEnvironment.DEVELOPMENT,
            okHttp = okHttp
        ) to recorder
    }

    private data class ScriptedResponse(
        val status: Int,
        val body: String
    )

    private class ScriptedInterceptor(private val responses: List<ScriptedResponse>) : Interceptor {

        val requests = CopyOnWriteArrayList<Request>()
        val bodies = CopyOnWriteArrayList<String>()
        private val index = AtomicInteger(0)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                bodies += buffer.readUtf8()
            }
            val scripted = responses.getOrElse(index.getAndIncrement()) { responses.last() }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(scripted.status)
                .message(if (scripted.status in 200..299) "OK" else "Error")
                .body(scripted.body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
