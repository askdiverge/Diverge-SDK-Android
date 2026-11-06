package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.ChatFormReferenceRemote
import ai.askdiverge.data.model.form.ChatFormTriggerRemote
import ai.askdiverge.data.model.form.FormFieldConditionOperatorRemote
import ai.askdiverge.data.model.form.FormFieldConditionRemote
import ai.askdiverge.data.model.form.FormFieldRemote
import ai.askdiverge.data.model.form.FormFieldTypeRemote
import ai.askdiverge.data.model.form.ShowContactFormRemote
import ai.askdiverge.data.model.form.ShowFormRemote
import ai.askdiverge.data.model.form.ShowSupportTicketRemote
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatFormReference
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.FormFieldCondition
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket

internal fun FormFieldRemote.mapToDomain() = FormField(
    key = key,
    label = label,
    placeholder = placeholder,
    type = type.toFormFieldType(),
    required = required ?: false,
    options = options.orEmpty(),
    visibleWhen = visible_when.orEmpty().map { it.mapToDomain() }
)

internal fun FormFieldConditionRemote.mapToDomain() = FormFieldCondition(
    field = field,
    operator = operator.toFormFieldConditionOperator(),
    value = value
)

internal fun ShowContactFormRemote.mapToDomain() = ShowContactForm(
    partId = part_id,
    fields = fields.map { it.mapToDomain() }
)

internal fun ShowSupportTicketRemote.mapToDomain() = ShowSupportTicket(
    partId = part_id,
    fields = fields.map { it.mapToDomain() },
    attachmentsAccepted = attachments_accepted,
    maxAttachmentSizeBytes = max_attachment_size_bytes
)

internal fun ShowFormRemote.mapToDomain() = ShowForm(
    partId = part_id,
    formId = form_id,
    name = name?.trimToNull(),
    confirmationText = confirmation_text,
    fields = fields.map { it.mapToDomain() },
    minFilledFields = min_filled_fields
)

internal fun ChatFormDefinitionRemote.mapToDomain() = ChatFormDefinition(
    formId = form_id,
    name = name?.trimToNull(),
    confirmationText = confirmation_text,
    fields = fields.map { it.mapToDomain() },
    minFilledFields = min_filled_fields
)

internal fun ChatFormReferenceRemote.mapToDomain() = ChatFormReference(
    formId = form_id,
    name = name?.trimToNull(),
    trigger = trigger.mapToDomain()
)

/**
 * Maps the wire type onto [FormField.FieldType]: `phone` → TEL, and a missing or unrecognised type
 * → TEXT so a required field always has a control.
 */
private fun FormFieldTypeRemote?.toFormFieldType(): FormField.FieldType = when (this) {
    null, FormFieldTypeRemote.TEXT, FormFieldTypeRemote.UNKNOWN -> FormField.FieldType.TEXT
    FormFieldTypeRemote.EMAIL -> FormField.FieldType.EMAIL
    FormFieldTypeRemote.TEL, FormFieldTypeRemote.PHONE -> FormField.FieldType.TEL
    FormFieldTypeRemote.TEXTAREA -> FormField.FieldType.TEXTAREA
    FormFieldTypeRemote.FILE -> FormField.FieldType.FILE
    FormFieldTypeRemote.DROPDOWN -> FormField.FieldType.DROPDOWN
}

/** Missing operator defaults to equals; unrecognised values map to UNKNOWN. */
private fun FormFieldConditionOperatorRemote?.toFormFieldConditionOperator(): FormFieldCondition.Operator =
    when (this) {
        null, FormFieldConditionOperatorRemote.EQUALS -> FormFieldCondition.Operator.EQUALS
        FormFieldConditionOperatorRemote.UNKNOWN -> FormFieldCondition.Operator.UNKNOWN
    }

private fun ChatFormTriggerRemote.mapToDomain(): ChatFormReference.Trigger = when (this) {
    ChatFormTriggerRemote.SESSION_START -> ChatFormReference.Trigger.SESSION_START
    ChatFormTriggerRemote.LIVECHAT_WAITING -> ChatFormReference.Trigger.LIVECHAT_WAITING
    ChatFormTriggerRemote.LLM -> ChatFormReference.Trigger.LLM
    ChatFormTriggerRemote.UNKNOWN -> ChatFormReference.Trigger.UNKNOWN
}

private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }
