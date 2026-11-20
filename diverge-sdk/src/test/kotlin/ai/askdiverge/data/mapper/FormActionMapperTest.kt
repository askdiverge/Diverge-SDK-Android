package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.form.ContactFormActionRemote
import ai.askdiverge.data.model.form.FormSubmissionActionRemote
import ai.askdiverge.data.model.form.OutgoingAttachmentRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.form.SupportTicketActionRemote
import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

internal class FormActionMapperTest {

    private val photo = OutgoingAttachment(
        dataBase64 = "QQ==",
        mimeType = "image/jpeg",
        filename = "photo.jpg"
    )

    private val photoRemote = OutgoingAttachmentRemote(
        filename = "photo.jpg",
        mime_type = "image/jpeg",
        data_base64 = "QQ=="
    )

    @Nested
    @DisplayName("SubmitActionRequest.mapToRemote")
    inner class RequestToRemote {

        @Test
        fun `a contact form keeps its part, page and field keys`() {
            val remote = makeSUT(
                currentPage = "Checkout",
                action = SubmitAction.ContactForm(fields = mapOf("orderNumber" to "ABC-123"))
            )

            assertEquals(
                SubmitActionRequestRemote(
                    part_id = "part_1",
                    current_page_url = "Checkout",
                    action = ContactFormActionRemote(fields = mapOf("orderNumber" to "ABC-123"))
                ),
                remote
            )
        }

        @Test
        fun `a ticket carries its attachments, and leaves them out when there are none`() {
            val withPhoto = makeSUT(
                action = SubmitAction.SupportTicket(
                    name = "Jane",
                    email = "jane@example.com",
                    message = "Broken",
                    attachments = listOf(photo)
                )
            )
            val withoutPhoto = makeSUT(
                action = SubmitAction.SupportTicket(
                    name = "Jane",
                    email = "jane@example.com",
                    message = "Broken",
                    attachments = emptyList()
                )
            )

            assertEquals(
                SupportTicketActionRemote(
                    name = "Jane",
                    email = "jane@example.com",
                    message = "Broken",
                    attachments = listOf(photoRemote)
                ),
                withPhoto.action
            )
            assertEquals(
                SupportTicketActionRemote(
                    name = "Jane",
                    email = "jane@example.com",
                    message = "Broken",
                    attachments = null
                ),
                withoutPhoto.action
            )
        }

        @Test
        fun `a form submission leaves out empty values and empty files`() {
            val valuesOnly = makeSUT(
                action = SubmitAction.FormSubmission(
                    formId = "salesLead",
                    values = mapOf("companyName" to "Acme"),
                    files = emptyMap()
                )
            )
            val filesOnly = makeSUT(
                action = SubmitAction.FormSubmission(
                    formId = "salesLead",
                    values = emptyMap(),
                    files = mapOf("photo" to photo)
                )
            )

            assertEquals(
                FormSubmissionActionRemote(
                    form_id = "salesLead",
                    values = mapOf("companyName" to "Acme"),
                    files = null
                ),
                valuesOnly.action
            )
            assertEquals(
                FormSubmissionActionRemote(
                    form_id = "salesLead",
                    values = null,
                    files = mapOf("photo" to photoRemote)
                ),
                filesOnly.action
            )
        }
    }

    @Nested
    @DisplayName("SubmitActionResponseRemote.mapToDomain")
    inner class ResponseToDomain {

        @Test
        fun `a response maps its submission id and confirmation text`() {
            val response = SubmitActionResponseRemote(
                submission_id = "sub_1",
                confirmation_text = "Thanks"
            ).mapToDomain()

            assertEquals(
                SubmitActionResponse(submissionId = "sub_1", confirmationText = "Thanks"),
                response
            )
        }

        @Test
        fun `a response without a submission id still maps as a success`() {
            val response = SubmitActionResponseRemote(
                submission_id = null,
                confirmation_text = null
            ).mapToDomain()

            assertEquals(
                SubmitActionResponse(submissionId = null, confirmationText = null),
                response
            )
        }
    }

    private fun makeSUT(
        action: SubmitAction,
        currentPage: String? = null
    ): SubmitActionRequestRemote = SubmitActionRequest(
        partId = "part_1",
        currentPage = currentPage,
        action = action
    ).mapToRemote()
}
