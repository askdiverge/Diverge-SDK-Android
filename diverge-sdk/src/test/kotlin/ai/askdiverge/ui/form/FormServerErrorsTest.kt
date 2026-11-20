package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.FormRequestException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FormServerErrorsTest {

    @Test
    fun `known field paths map onto field errors`() {
        val applied = FormServerErrors.apply(
            params = listOf(
                FormRequestException.Validation.Param(field = "action.fields.email", message = "bad"),
                FormRequestException.Validation.Param(field = "action.values.company", message = "bad")
            ),
            knownFieldKeys = setOf("email", "company")
        )

        assertEquals(FormFieldError.REJECTED, applied.fieldErrors["email"])
        assertEquals(FormFieldError.REJECTED, applied.fieldErrors["company"])
        assertNull(applied.formError)
    }

    @Test
    fun `unknown paths raise a banner error`() {
        val applied = FormServerErrors.apply(
            params = listOf(
                FormRequestException.Validation.Param(field = "action.unknown", message = "bad")
            ),
            knownFieldKeys = setOf("email")
        )

        assertTrue(applied.fieldErrors.isEmpty())
        assertEquals(FormBannerError.SubmitFailed, applied.formError)
    }

    @Test
    fun `empty params still show a banner`() {
        val applied = FormServerErrors.apply(params = emptyList(), knownFieldKeys = setOf("email"))
        assertEquals(FormBannerError.SubmitFailed, applied.formError)
    }

    @Test
    fun `fieldKey strips known prefixes`() {
        assertEquals("email", FormServerErrors.fieldKey("action.fields.email"))
        assertEquals("email", FormServerErrors.fieldKey("action.values.email"))
        assertEquals("photo", FormServerErrors.fieldKey("action.files.photo"))
        assertEquals("email", FormServerErrors.fieldKey("action.email"))
        assertEquals("email", FormServerErrors.fieldKey("values.email"))
        assertEquals("raw", FormServerErrors.fieldKey("raw"))
    }

    @Test
    fun `values patch paths map onto field errors`() {
        val applied = FormServerErrors.apply(
            params = listOf(
                FormRequestException.Validation.Param(field = "values.email", message = "bad")
            ),
            knownFieldKeys = setOf("email")
        )

        assertEquals(FormFieldError.REJECTED, applied.fieldErrors["email"])
        assertNull(applied.formError)
    }
}
