package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatSessionState
import ai.askdiverge.domain.model.form.ChatSessionValue
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.FormFieldCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class WaitingFormModelTest {

    @Test
    fun `cardFields omits file fields and rejects a required file`() {
        val definition = definition(
            FormField(
                key = "email",
                label = "Email",
                placeholder = null,
                type = FormField.FieldType.EMAIL,
                required = true,
                options = emptyList(),
                visibleWhen = emptyList()
            ),
            FormField(
                key = "photo",
                label = "Photo",
                placeholder = null,
                type = FormField.FieldType.FILE,
                required = false,
                options = emptyList(),
                visibleWhen = emptyList()
            )
        )

        val fields = assertNotNull(WaitingFormModel.cardFields(definition))
        assertEquals(listOf("email"), fields.map { it.key })
        assertNull(
            WaitingFormModel.cardFields(
                definition(
                    FormField(
                        key = "photo",
                        label = "Photo",
                        placeholder = null,
                        type = FormField.FieldType.FILE,
                        required = true,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                )
            )
        )
    }

    @Test
    fun `cardFields rejects a required field behind a condition`() {
        val definition = definition(
            FormField(
                key = "topic",
                label = "Topic",
                placeholder = null,
                type = FormField.FieldType.TEXT,
                required = true,
                options = emptyList(),
                visibleWhen = emptyList()
            ),
            FormField(
                key = "detail",
                label = "Detail",
                placeholder = null,
                type = FormField.FieldType.TEXT,
                required = true,
                options = emptyList(),
                visibleWhen = listOf(
                    FormFieldCondition(
                        field = "topic",
                        operator = FormFieldCondition.Operator.EQUALS,
                        value = "other"
                    )
                )
            )
        )

        assertNull(WaitingFormModel.cardFields(definition))
    }

    @Test
    fun `valuesForPatch sends only trimmed non-empty visible values`() {
        val model = assertNotNull(
            WaitingFormModel.create(
                definition = definition(
                    FormField(
                        key = "email",
                        label = "Email",
                        placeholder = null,
                        type = FormField.FieldType.EMAIL,
                        required = false,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    ),
                    FormField(
                        key = "name",
                        label = "Name",
                        placeholder = null,
                        type = FormField.FieldType.TEXT,
                        required = false,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                )
            )
        ).setValue(value = "  a@b.c  ", key = "email")
            .setValue(value = "   ", key = "name")

        assertEquals(mapOf("email" to "a@b.c"), model.valuesForPatch())
    }

    @Test
    fun `canSave is true only when the patch differs from the session`() {
        val model = assertNotNull(
            WaitingFormModel.create(
                definition = definition(
                    FormField(
                        key = "email",
                        label = "Email",
                        placeholder = null,
                        type = FormField.FieldType.EMAIL,
                        required = false,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                ),
                sessionValues = listOf(ChatSessionValue(key = "email", value = "a@b.c"))
            )
        )

        assertFalse(model.canSave)
        assertTrue(model.setValue(value = "other@b.c", key = "email").canSave)
    }

    @Test
    fun `markSaved adopts the returned session values`() {
        val model = assertNotNull(
            WaitingFormModel.create(
                definition = definition(
                    FormField(
                        key = "email",
                        label = "Email",
                        placeholder = null,
                        type = FormField.FieldType.EMAIL,
                        required = false,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                )
            )
        ).setValue(value = "a@b.c", key = "email")

        val saved = model.markSaved(
            ChatSessionState(values = listOf(ChatSessionValue(key = "email", value = "a@b.c")))
        )

        assertTrue(saved.isSaved)
        assertEquals("a@b.c", saved.values["email"])
        assertFalse(saved.canSave)
    }

    private fun definition(vararg fields: FormField) = ChatFormDefinition(
        formId = "livechatWaitingContact",
        name = "Waiting",
        confirmationText = null,
        fields = fields.toList(),
        minFilledFields = 0
    )
}
