package ai.askdiverge.data.model.form

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatFormReference
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Pins the `/config` `forms[]` row and the `GET /api/v1/chat/forms/{form_id}` body against the
 * shapes the Chatbot API documents.
 */
class ChatFormDecodingTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val referenceAdapter = moshi.adapter(ChatFormReferenceRemote::class.java)
    private val definitionAdapter = moshi.adapter(ChatFormDefinitionRemote::class.java)

    @Nested
    @DisplayName("ChatFormReference")
    inner class Reference {

        @Test
        fun `a config form row decodes its id, name and trigger`() {
            val row = decodeReference(
                """
                {
                  "trigger": "livechat_waiting",
                  "form_id": "livechatWaitingContact",
                  "name": "Waiting contact",
                  "override_targets": [],
                  "submit_actions": []
                }
                """.trimIndent()
            )

            assertEquals("livechatWaitingContact", row.formId)
            assertEquals("Waiting contact", row.name)
            assertEquals(ChatFormReference.Trigger.LIVECHAT_WAITING, row.trigger)
        }

        @Test
        fun `a config form row with a null name is unnamed`() {
            val row = decodeReference(
                """{"form_id":"frm_1","name":null,"trigger":"session_start"}"""
            )

            assertNull(row.name)
            assertEquals(ChatFormReference.Trigger.SESSION_START, row.trigger)
        }

        @Test
        fun `a config form row name is trimmed, and a blank one is unnamed`() {
            val named = decodeReference(
                """{"form_id":"frm_1","name":"  Waiting contact \n","trigger":"llm"}"""
            )
            val blank = decodeReference("""{"form_id":"frm_1","name":"   ","trigger":"llm"}""")

            assertEquals("Waiting contact", named.name)
            assertNull(blank.name)
        }

        @Test
        fun `every wire trigger decodes to its moment`() {
            val triggers = mapOf(
                "session_start" to ChatFormReference.Trigger.SESSION_START,
                "livechat_waiting" to ChatFormReference.Trigger.LIVECHAT_WAITING,
                "llm" to ChatFormReference.Trigger.LLM
            )

            triggers.forEach { (wire, trigger) ->
                val row = decodeReference("""{"form_id":"frm_1","trigger":"$wire"}""")

                assertEquals(trigger, row.trigger, wire)
            }
        }

        @Test
        fun `an unrecognised trigger decodes to unknown`() {
            val row = decodeReference(
                """{"form_id":"frm_1","trigger":"some_future_trigger"}"""
            )

            assertEquals(ChatFormReference.Trigger.UNKNOWN, row.trigger)
        }

        @Test
        fun `a config form row without a trigger fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeReference("""{"form_id":"frm_1"}""")
            }
        }
    }

    @Nested
    @DisplayName("ChatFormDefinition")
    inner class Definition {

        @Test
        fun `a form definition decodes its fields and minimum filled count`() {
            val definition = decodeDefinition(
                """
                {
                  "form_id": "frm_1",
                  "name": "Callback",
                  "confirmation_text": "Thanks, we will call you back.",
                  "fields": [
                    { "key": "name", "label": "Your name" },
                    { "key": "phone", "label": "Phone", "type": "tel" }
                  ],
                  "min_filled_fields": 1
                }
                """.trimIndent()
            )

            assertEquals("frm_1", definition.formId)
            assertEquals("Callback", definition.name)
            assertEquals("Thanks, we will call you back.", definition.confirmationText)
            assertEquals(listOf("name", "phone"), definition.fields.map { it.key })
            assertEquals(1, definition.minFilledFields)
        }

        @Test
        fun `a form definition with a null name is unnamed and has no confirmation copy`() {
            val definition = decodeDefinition(
                """{"form_id":"frm_1","name":null,"fields":[],"min_filled_fields":0}"""
            )

            assertNull(definition.name)
            assertNull(definition.confirmationText)
        }

        @Test
        fun `a form definition name is trimmed, and a blank one is unnamed`() {
            val named = decodeDefinition(
                """{"form_id":"frm_1","name":"  Callback \n","fields":[],"min_filled_fields":0}"""
            )
            val blank = decodeDefinition(
                """{"form_id":"frm_1","name":"   ","fields":[],"min_filled_fields":0}"""
            )

            assertEquals("Callback", named.name)
            assertNull(blank.name)
        }

        @Test
        fun `a form definition without fields fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeDefinition("""{"form_id":"frm_1","name":null,"min_filled_fields":0}""")
            }
        }
    }

    private fun decodeReference(json: String): ChatFormReference =
        requireNotNull(referenceAdapter.fromJson(json)).mapToDomain()

    private fun decodeDefinition(json: String): ChatFormDefinition =
        requireNotNull(definitionAdapter.fromJson(json)).mapToDomain()
}
