package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.FormFieldCondition
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FormValidatorTest {

    private val email = field(key = "email", type = FormField.FieldType.EMAIL, required = true)
    private val name = field(key = "name", required = true)
    private val contact = ConversationForm.contact(
        ShowContactForm(partId = "p1", fields = listOf(name, email))
    )

    @Test
    fun `required empty fields fail validation`() {
        val result = FormValidator.validate(contact, emptyMap())

        assertFalse(result.isValid)
        assertEquals(FormFieldError.REQUIRED, result.fieldErrors["name"])
        assertEquals(FormFieldError.REQUIRED, result.fieldErrors["email"])
    }

    @Test
    fun `invalid email is rejected`() {
        val result = FormValidator.validate(
            contact,
            mapOf("name" to "Ada", "email" to "not-an-email")
        )

        assertEquals(FormFieldError.INVALID_EMAIL, result.fieldErrors["email"])
    }

    @Test
    fun `visible_when hides dependent fields until the parent matches`() {
        val topic = field(key = "topic", type = FormField.FieldType.DROPDOWN, required = true)
        val detail = field(
            key = "detail",
            required = true,
            visibleWhen = listOf(
                FormFieldCondition(
                    field = "topic",
                    operator = FormFieldCondition.Operator.EQUALS,
                    value = "Other"
                )
            )
        )
        val form = ConversationForm.custom(
            ShowForm(
                partId = "p2",
                formId = "f1",
                name = null,
                confirmationText = null,
                fields = listOf(topic, detail),
                minFilledFields = 0
            )
        )

        assertEquals(
            expected = listOf(topic),
            actual = FormValidator.visibleFields(form, mapOf("topic" to "Billing"))
        )
        assertEquals(
            expected = listOf(topic, detail),
            actual = FormValidator.visibleFields(form, mapOf("topic" to "Other"))
        )
    }

    @Test
    fun `required file field blocks completion when attach is not offered`() {
        val file = field(key = "photo", type = FormField.FieldType.FILE, required = true)
        val form = ConversationForm.custom(
            ShowForm(
                partId = "p3",
                formId = "with-file",
                name = null,
                confirmationText = null,
                fields = listOf(file),
                minFilledFields = 0
            )
        )

        assertEquals(file, FormValidator.blockingField(form, emptyMap(), offersFileFields = false))
        assertTrue(
            FormValidator.validate(form, emptyMap(), offersFileFields = false)
                .formError is FormBannerError.Unavailable
        )
    }

    @Test
    fun `required file field validates when attach is offered`() {
        val file = field(key = "photo", type = FormField.FieldType.FILE, required = true)
        val form = ConversationForm.custom(
            ShowForm(
                partId = "p3b",
                formId = "with-file",
                name = null,
                confirmationText = null,
                fields = listOf(file),
                minFilledFields = 0
            )
        )

        assertNull(FormValidator.blockingField(form, emptyMap(), offersFileFields = true))
        val missing = FormValidator.validate(form, emptyMap(), files = emptySet(), offersFileFields = true)
        assertEquals(FormFieldError.REQUIRED, missing.fieldErrors["photo"])

        val filled = FormValidator.validate(
            form,
            emptyMap(),
            files = setOf("photo"),
            offersFileFields = true
        )
        assertTrue(filled.isValid)
    }

    @Test
    fun `contact form never treats file fields as fillable`() {
        val file = field(key = "photo", type = FormField.FieldType.FILE, required = true)
        val form = ConversationForm.contact(
            ShowContactForm(partId = "p3c", fields = listOf(file))
        )

        assertEquals(file, FormValidator.blockingField(form, emptyMap(), offersFileFields = true))
    }

    @Test
    fun `custom form enforces min filled fields`() {
        val a = field(key = "a", required = false)
        val b = field(key = "b", required = false)
        val form = ConversationForm.custom(
            ShowForm(
                partId = "p4",
                formId = "f2",
                name = null,
                confirmationText = null,
                fields = listOf(a, b),
                minFilledFields = 2
            )
        )

        val result = FormValidator.validate(form, mapOf("a" to "x"))
        assertFalse(result.isValid)
        assertEquals(FormBannerError.MinFilled(2), result.formError)
        assertNull(result.fieldErrors["a"])
    }

    private fun field(
        key: String,
        type: FormField.FieldType = FormField.FieldType.TEXT,
        required: Boolean = false,
        visibleWhen: List<FormFieldCondition> = emptyList()
    ) = FormField(
        key = key,
        label = key,
        placeholder = null,
        type = type,
        required = required,
        options = emptyList(),
        visibleWhen = visibleWhen
    )
}
