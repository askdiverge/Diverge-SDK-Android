package ai.askdiverge.data.model.form

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.FormFieldCondition
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
 * Pins how a form field and its visibility conditions decode from the shapes form markers carry
 * inline. A field that decodes differently from what the operator configured changes what the
 * visitor is asked to fill in.
 */
class FormFieldDecodingTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val adapter = moshi.adapter(FormFieldRemote::class.java)

    @Nested
    @DisplayName("FormField — decoding against the API contract")
    inner class Contract {

        @Test
        fun `a field with only a key and label takes the contract defaults`() {
            val field = decodeField("""{"key":"name","label":"Your name"}""")

            assertEquals("name", field.key)
            assertEquals("Your name", field.label)
            assertNull(field.placeholder)
            assertEquals(FormField.FieldType.TEXT, field.type)
            assertFalse(field.required)
            assertTrue(field.options.isEmpty())
            assertTrue(field.visibleWhen.isEmpty())
        }

        @Test
        fun `the deprecated phone type decodes as tel`() {
            val field = decodeField("""{"key":"phone","label":"Phone","type":"phone"}""")

            assertEquals(FormField.FieldType.TEL, field.type)
        }

        @Test
        fun `every wire type decodes to its control`() {
            val types = mapOf(
                "text" to FormField.FieldType.TEXT,
                "email" to FormField.FieldType.EMAIL,
                "tel" to FormField.FieldType.TEL,
                "textarea" to FormField.FieldType.TEXTAREA,
                "file" to FormField.FieldType.FILE,
                "dropdown" to FormField.FieldType.DROPDOWN
            )

            types.forEach { (wire, type) ->
                val field = decodeField("""{"key":"answer","label":"Answer","type":"$wire"}""")

                assertEquals(type, field.type, wire)
            }
        }

        @Test
        fun `an unrecognised type falls back to text`() {
            val field = decodeField("""{"key":"when","label":"When","type":"some_future_type"}""")

            assertEquals(FormField.FieldType.TEXT, field.type)
        }

        @Test
        fun `a null type and operator take the contract defaults`() {
            val field = decodeField(
                """{"key":"plan","label":"Plan","type":null,"visible_when":[{"field":"topic","operator":null,"value":"billing"}]}"""
            )

            assertEquals(FormField.FieldType.TEXT, field.type)
            assertEquals(FormFieldCondition.Operator.EQUALS, field.visibleWhen.first().operator)
        }

        @Test
        fun `a dropdown decodes its options and visibility condition`() {
            val field = decodeField(
                """
                {
                  "key": "plan",
                  "label": "Plan",
                  "placeholder": "Pick a plan",
                  "type": "dropdown",
                  "required": true,
                  "options": ["Basic", "Pro"],
                  "visible_when": [{ "field": "topic", "operator": "equals", "value": "billing" }]
                }
                """.trimIndent()
            )

            assertEquals("Pick a plan", field.placeholder)
            assertEquals(FormField.FieldType.DROPDOWN, field.type)
            assertTrue(field.required)
            assertEquals(listOf("Basic", "Pro"), field.options)
            val condition = field.visibleWhen.first()
            assertEquals("topic", condition.field)
            assertEquals(FormFieldCondition.Operator.EQUALS, condition.operator)
            assertEquals("billing", condition.value)
        }

        @Test
        fun `a condition without an operator compares for equality`() {
            val field = decodeField(
                """{"key":"plan","label":"Plan","visible_when":[{"field":"topic","value":"billing"}]}"""
            )

            assertEquals(FormFieldCondition.Operator.EQUALS, field.visibleWhen.first().operator)
        }

        @Test
        fun `an unrecognised condition operator decodes to unknown`() {
            val field = decodeField(
                """
                {"key":"plan","label":"Plan","visible_when":[{"field":"topic","operator":"some_future_operator","value":"billing"}]}
                """.trimIndent()
            )

            assertEquals(FormFieldCondition.Operator.UNKNOWN, field.visibleWhen.first().operator)
        }

        @Test
        fun `a malformed condition fails the field`() {
            assertFailsWith<JsonDataException> {
                decodeField("""{"key":"plan","label":"Plan","visible_when":[{"field":"topic"}]}""")
            }
        }

        @Test
        fun `a field without a label fails to decode`() {
            assertFailsWith<JsonDataException> {
                decodeField("""{"key":"email","label":null}""")
            }
        }
    }

    private fun decodeField(json: String): FormField =
        requireNotNull(adapter.fromJson(json)).mapToDomain()
}
