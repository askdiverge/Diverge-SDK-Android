package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.model.form.ShowContactFormRemote
import ai.askdiverge.data.model.form.ShowFormRemote
import ai.askdiverge.data.model.form.ShowSupportTicketRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Pins that the three form markers decode as message parts by their `type`, through the Moshi
 * setup the SDK builds, and map to their domain markers.
 */
class FormMarkerPartDecodingTest {

    private val messageAdapter = Moshi.Builder().buildConversationModels().build().adapter(MessageRemote::class.java)

    @Test
    fun `a message decodes each form marker part by its type, beside its other parts`() {
        val message = assertNotNull(
            messageAdapter.fromJson(
                """
                {
                  "message_id": "m1",
                  "role": "assistant",
                  "parts": [
                    {"type": "rich_text", "part_id": "p1", "blocks": [{"type": "paragraph", "spans": [{"text": "Hi", "type": "text"}]}]},
                    {"type": "show_contact_form", "part_id": "p2", "fields": [{"key": "email", "label": "Email", "type": "email", "required": true}]},
                    {"type": "show_support_ticket", "part_id": "p3", "fields": [{"key": "name", "label": "Name"}], "attachments_accepted": true, "max_attachment_size_bytes": 1048576},
                    {"type": "show_form", "part_id": "p4", "form_id": "claim"}
                  ]
                }
                """.trimIndent()
            )
        )

        assertEquals(expected = 4, actual = message.parts.size)
        assertIs<RichTextPartRemote>(message.parts[0])
        assertEquals(expected = "p2", actual = assertIs<ShowContactFormRemote>(message.parts[1]).part_id)
        assertEquals(expected = "p3", actual = assertIs<ShowSupportTicketRemote>(message.parts[2]).part_id)
        assertEquals(expected = "claim", actual = assertIs<ShowFormRemote>(message.parts[3]).form_id)
    }

    @Test
    fun `decoded form marker parts map to their domain markers`() {
        val message = assertNotNull(
            messageAdapter.fromJson(
                """
                {
                  "message_id": "m1",
                  "role": "assistant",
                  "parts": [
                    {"type": "show_contact_form", "part_id": "p2", "fields": []},
                    {"type": "show_support_ticket", "part_id": "p3", "fields": [], "attachments_accepted": false, "max_attachment_size_bytes": 0},
                    {"type": "show_form", "part_id": "p4", "form_id": "claim", "name": " Claim "}
                  ]
                }
                """.trimIndent()
            )
        )

        val parts = message.parts.map { it.mapToDomain() }

        assertIs<ShowContactForm>(parts[0])
        assertIs<ShowSupportTicket>(parts[1])
        assertEquals(expected = "Claim", actual = assertIs<ShowForm>(parts[2]).name)
    }
}
