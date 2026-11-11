package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class FormSubmissionModelTest {

    private val contact = ConversationForm.contact(
        ShowContactForm(
            partId = "p1",
            fields = listOf(
                FormField(
                    key = "email",
                    label = "Email",
                    placeholder = null,
                    type = FormField.FieldType.EMAIL,
                    required = true,
                    options = emptyList(),
                    visibleWhen = emptyList()
                )
            )
        )
    )

    @Test
    fun `setValue clears that field error and leaves others`() {
        val model = FormSubmissionModel(
            form = contact,
            fieldErrors = mapOf("email" to FormFieldError.REQUIRED, "name" to FormFieldError.REQUIRED)
        ).setValue(value = "a@b.c", key = "email")

        assertEquals("a@b.c", model.values["email"])
        assertFalse(model.fieldErrors.containsKey("email"))
        assertEquals(FormFieldError.REQUIRED, model.fieldErrors["name"])
    }

    @Test
    fun `409 conflict becomes already sent confirmation`() {
        val submitted = FormSubmissionModel(form = contact).markAlreadySent()
        val phase = assertIs<FormSubmissionModel.Phase.Submitted>(submitted.phase)
        assertEquals(FormSubmissionModel.ALREADY_SENT_TOKEN, phase.confirmation)
    }

    @Test
    fun `changing the ticket attachment clears the form banner`() {
        val model = FormSubmissionModel(form = contact, formError = FormBannerError.SubmitFailed)
            .setTicketAttachment(null)

        assertEquals(null, model.formError)
    }

    @Test
    fun `server errors return the draft to editing`() {
        val model = FormSubmissionModel(form = contact, phase = FormSubmissionModel.Phase.Submitting)
            .applyServerErrors(
                listOf(FormRequestException.Validation.Param(field = "action.fields.email", message = "x"))
            )

        assertIs<FormSubmissionModel.Phase.Editing>(model.phase)
        assertEquals(FormFieldError.REJECTED, model.fieldErrors["email"])
    }

    @Test
    fun `an unnamed custom form is titled by its form id, and contact forms carry no name`() {
        val unnamed = ConversationForm.custom(
            ShowForm(
                partId = "p2",
                formId = "claim-form",
                name = null,
                confirmationText = null,
                fields = emptyList(),
                minFilledFields = 0
            )
        )

        assertEquals("claim-form", unnamed.displayName)
        assertEquals(null, contact.displayName)
    }

    @Test
    fun `thin custom form hydrates fields from the definition`() {
        val thin = ConversationForm.custom(
            ShowForm(
                partId = "p2",
                formId = "lead",
                name = null,
                confirmationText = null,
                fields = emptyList(),
                minFilledFields = 0
            )
        )
        val field = FormField(
            key = "company",
            label = "Company",
            placeholder = null,
            type = FormField.FieldType.TEXT,
            required = true,
            options = emptyList(),
            visibleWhen = emptyList()
        )
        val model = FormSubmissionModel(form = thin)
            .beginHydrating()
            .applyHydratedDefinition(
                ChatFormDefinition(
                    formId = "lead",
                    name = "Sales lead",
                    confirmationText = "Got it",
                    fields = listOf(field),
                    minFilledFields = 1
                )
            )

        assertFalse(model.isHydrating)
        assertEquals("Sales lead", model.form.displayName)
        assertEquals(listOf(field), model.form.fields)
        assertEquals(1, model.form.minFilledFields)
        assertTrue(model.hasFields)
    }

    @Test
    fun `confirmation prefers server text then form then default`() {
        assertEquals(
            "From server",
            FormSubmissionModel.confirmation("From server", contact, "Default")
        )
        val withInline = ConversationForm.custom(
            ShowForm(
                partId = "p3",
                formId = "f",
                name = null,
                confirmationText = "From form",
                fields = emptyList(),
                minFilledFields = 0
            )
        )
        assertEquals(
            "From form",
            FormSubmissionModel.confirmation("  ", withInline, "Default")
        )
        assertEquals("Default", FormSubmissionModel.confirmation(null, contact, "Default"))
    }
}
