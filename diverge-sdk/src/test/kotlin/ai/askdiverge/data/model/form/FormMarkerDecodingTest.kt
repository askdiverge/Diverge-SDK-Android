package ai.askdiverge.data.model.form

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Pins how the contact-form, support-ticket and custom-form markers decode: every required key
 * must be present, and a `show_form` needs only its `part_id` and `form_id`.
 */
class FormMarkerDecodingTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val contactAdapter = moshi.adapter(ShowContactFormRemote::class.java)
    private val ticketAdapter = moshi.adapter(ShowSupportTicketRemote::class.java)
    private val formAdapter = moshi.adapter(ShowFormRemote::class.java)

    @Nested
    @DisplayName("show_contact_form")
    inner class ContactForm {

        @Test
        fun `a contact form decodes its inline fields in order`() {
            val form = decodeContact(
                """
                {
                  "type": "show_contact_form",
                  "part_id": "part_2",
                  "fields": [
                    { "key": "name", "label": "Your name", "required": true },
                    { "key": "email", "label": "Email address", "type": "email", "required": true }
                  ]
                }
                """.trimIndent()
            )

            assertEquals("part_2", form.partId)
            assertEquals(listOf("name", "email"), form.fields.map { it.key })
            assertEquals(
                listOf(FormField.FieldType.TEXT, FormField.FieldType.EMAIL),
                form.fields.map { it.type }
            )
        }

        @Test
        fun `a contact form without fields fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeContact("""{"type":"show_contact_form","part_id":"p_1"}""")
            }
        }

        @Test
        fun `a contact form with a malformed field fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeContact(
                    """{"type":"show_contact_form","part_id":"p_1","fields":[{"key":"email","label":null}]}"""
                )
            }
        }

        @Test
        fun `a contact form without part_id fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeContact("""{"type":"show_contact_form","fields":[]}""")
            }
        }
    }

    @Nested
    @DisplayName("show_support_ticket")
    inner class SupportTicket {

        @Test
        fun `a support ticket without its attachment flags fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeTicket("""{"type":"show_support_ticket","part_id":"p_1","fields":[]}""")
            }
        }

        @Test
        fun `a support ticket decodes its fields and attachment flags`() {
            val ticket = decodeTicket(
                """
                {
                  "type": "show_support_ticket",
                  "part_id": "p_1",
                  "fields": [{ "key": "message", "label": "Message", "type": "textarea" }],
                  "attachments_accepted": false,
                  "max_attachment_size_bytes": 5242880
                }
                """.trimIndent()
            )

            assertEquals(listOf("message"), ticket.fields.map { it.key })
            assertFalse(ticket.attachmentsAccepted)
            assertEquals(5_242_880, ticket.maxAttachmentSizeBytes)
        }
    }

    @Nested
    @DisplayName("show_form")
    inner class CustomForm {

        @Test
        fun `a thin show_form takes the defaults, for the client to hydrate`() {
            val form = decodeForm("""{"type":"show_form","form_id":"frm_1","part_id":"p_1"}""")

            assertEquals("p_1", form.partId)
            assertEquals("frm_1", form.formId)
            assertNull(form.name)
            assertTrue(form.fields.isEmpty())
            assertEquals(0, form.minFilledFields)
        }

        @Test
        fun `a show_form with empty fields decodes, for the client to hydrate`() {
            val form = decodeForm(
                """{"type":"show_form","form_id":"frm_1","part_id":"p_1","name":"Callback","fields":[],"min_filled_fields":0}"""
            )

            assertEquals("p_1", form.partId)
            assertEquals("frm_1", form.formId)
            assertNull(form.confirmationText)
            assertTrue(form.fields.isEmpty())
        }

        @Test
        fun `a show_form name is trimmed, and a blank one decodes as unnamed`() {
            val named = decodeForm(
                """{"type":"show_form","form_id":"frm_1","part_id":"p_1","name":"  Waiting contact \n","fields":[],"min_filled_fields":0}"""
            )
            val blank = decodeForm(
                """{"type":"show_form","form_id":"frm_1","part_id":"p_1","name":"   ","fields":[],"min_filled_fields":0}"""
            )

            assertEquals("Waiting contact", named.name)
            assertNull(blank.name)
        }

        @Test
        fun `a full show_form decodes its inline definition`() {
            val form = decodeForm(
                """
                {
                  "type": "show_form",
                  "form_id": "frm_1",
                  "part_id": "p_1",
                  "name": "Callback",
                  "confirmation_text": "Thanks, we will call you back.",
                  "fields": [{ "key": "phone", "label": "Phone", "type": "tel" }],
                  "min_filled_fields": 1
                }
                """.trimIndent()
            )

            assertEquals("Callback", form.name)
            assertEquals("Thanks, we will call you back.", form.confirmationText)
            assertEquals(listOf("phone"), form.fields.map { it.key })
            assertEquals(1, form.minFilledFields)
        }

        @Test
        fun `a show_form without form_id fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeForm("""{"type":"show_form","part_id":"p_1"}""")
            }
        }

        @Test
        fun `a show_form without part_id fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeForm("""{"type":"show_form","form_id":"frm_1"}""")
            }
        }
    }

    private fun decodeContact(json: String): ShowContactForm =
        requireNotNull(contactAdapter.fromJson(json)).mapToDomain()

    private fun decodeTicket(json: String): ShowSupportTicket =
        requireNotNull(ticketAdapter.fromJson(json)).mapToDomain()

    private fun decodeForm(json: String): ShowForm =
        requireNotNull(formAdapter.fromJson(json)).mapToDomain()
}
