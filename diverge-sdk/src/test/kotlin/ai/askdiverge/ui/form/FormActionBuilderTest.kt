package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket
import ai.askdiverge.domain.model.form.SubmitAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class FormActionBuilderTest {

    @Test
    fun `contact form builds a contact action with trimmed answers`() {
        val form = ConversationForm.contact(
            ShowContactForm(
                partId = "part_1",
                fields = listOf(text("email"), text("name"))
            )
        )

        val request = FormActionBuilder.build(
            form = form,
            values = mapOf("email" to "  a@b.c  ", "name" to " Ada "),
            currentPage = "/help"
        )

        assertNotNull(request)
        assertEquals("part_1", request.partId)
        assertEquals("/help", request.currentPage)
        val action = assertIs<SubmitAction.ContactForm>(request.action)
        assertEquals(mapOf("email" to "a@b.c", "name" to "Ada"), action.fields)
    }

    @Test
    fun `support ticket requires name email and message`() {
        val form = assertNotNull(
            ConversationForm.supportTicket(
                ShowSupportTicket(
                    partId = "part_2",
                    fields = listOf(text("name"), text("email"), text("message")),
                    attachmentsAccepted = false,
                    maxAttachmentSizeBytes = 0
                )
            )
        )

        assertNull(
            FormActionBuilder.build(form, mapOf("name" to "Ada", "email" to "a@b.c"))
        )

        val request = FormActionBuilder.build(
            form,
            mapOf("name" to "Ada", "email" to "a@b.c", "message" to "Broken")
        )
        val action = assertIs<SubmitAction.SupportTicket>(request!!.action)
        assertEquals("Broken", action.message)
    }

    @Test
    fun `custom form carries form id and values`() {
        val form = ConversationForm.custom(
            ShowForm(
                partId = "part_3",
                formId = "salesLead",
                name = "Sales",
                confirmationText = null,
                fields = listOf(text("company")),
                minFilledFields = 0
            )
        )

        val request = FormActionBuilder.build(form, mapOf("company" to "Acme"))
        val action = assertIs<SubmitAction.FormSubmission>(request!!.action)
        assertEquals("salesLead", action.formId)
        assertEquals(mapOf("company" to "Acme"), action.values)
    }

    private fun text(key: String) = FormField(
        key = key,
        label = key,
        placeholder = null,
        type = FormField.FieldType.TEXT,
        required = true,
        options = emptyList(),
        visibleWhen = emptyList()
    )
}
