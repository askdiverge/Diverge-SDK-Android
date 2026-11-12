package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.config.theme.header.HeaderAlignmentRemote
import ai.askdiverge.data.model.event.delta.DeltaActionRemote
import ai.askdiverge.data.model.event.delta.DeltaBlockTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaPartTypeRemote
import ai.askdiverge.data.model.form.ChatFormReferenceRemote
import ai.askdiverge.data.model.form.ChatFormTriggerRemote
import ai.askdiverge.data.model.form.ContactFormActionRemote
import ai.askdiverge.data.model.form.FormFieldConditionOperatorRemote
import ai.askdiverge.data.model.form.FormFieldTypeRemote
import ai.askdiverge.data.model.form.FormSubmissionActionRemote
import ai.askdiverge.data.model.form.ShowContactFormRemote
import ai.askdiverge.data.model.form.ShowFormRemote
import ai.askdiverge.data.model.form.ShowSupportTicketRemote
import ai.askdiverge.data.model.form.SubmitActionRemote
import ai.askdiverge.data.model.form.SubmitActionTypeRemote
import ai.askdiverge.data.model.form.SupportTicketActionRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityReasonRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityStatusRemote
import ai.askdiverge.data.model.livechat.LivechatClosedByRemote
import ai.askdiverge.data.model.livechat.LivechatFeedbackStatusRemote
import ai.askdiverge.data.model.livechat.LivechatMessageRemote
import ai.askdiverge.data.model.livechat.LivechatStateStatusRemote
import ai.askdiverge.data.model.livechat.RequestHumanAgentRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.BlockTypeRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.MessageFileRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartTypeRemote
import ai.askdiverge.data.model.message.incoming.part.ProductPartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.TablePartRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableCellBlockTypeRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellImageBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingImagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageTypeRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

private const val MESSAGE_PART_TYPE = "type"
private const val CONTENT_TYPE = "type"
private const val RICH_TEXT_BLOCK_TYPE = "type"
private const val TABLE_CELL_BLOCK_TYPE = "type"
private const val SUBMIT_ACTION_TYPE = "type"

internal fun Moshi.Builder.buildConversationModels(): Moshi.Builder = add(
    SkipUnknownInListAdapterFactory(
        targetTypes = setOf(
            MessagePartRemote::class.java,
            LivechatMessageRemote::class.java,
            RichTextBlockRemote::class.java,
            TableCellBlockRemote::class.java,
            ChatFormReferenceRemote::class.java
        )
    )
)
    .add(
        PolymorphicJsonAdapterFactory
            .of(OutgoingMessagePartRemote::class.java, MESSAGE_PART_TYPE)
            .withSubtype(OutgoingTextPartRemote::class.java, OutgoingMessageTypeRemote.TEXT.type)
            .withSubtype(OutgoingImagePartRemote::class.java, OutgoingMessageTypeRemote.IMAGE.type)
    )
    .add(
        PolymorphicJsonAdapterFactory
            .of(SubmitActionRemote::class.java, SUBMIT_ACTION_TYPE)
            .withSubtype(ContactFormActionRemote::class.java, SubmitActionTypeRemote.CONTACT_FORM)
            .withSubtype(SupportTicketActionRemote::class.java, SubmitActionTypeRemote.SUPPORT_TICKET)
            .withSubtype(FormSubmissionActionRemote::class.java, SubmitActionTypeRemote.FORM_SUBMISSION)
    )
    .add(
        PolymorphicJsonAdapterFactory.of(MessagePartRemote::class.java, CONTENT_TYPE)
            .withSubtype(RichTextPartRemote::class.java, MessagePartTypeRemote.RICH_TEXT.type)
            .withSubtype(ImagePartRemote::class.java, MessagePartTypeRemote.IMAGE.type)
            .withSubtype(ProductPartRemote::class.java, MessagePartTypeRemote.PRODUCTS.type)
            .withSubtype(TablePartRemote::class.java, MessagePartTypeRemote.TABLE.type)
            .withSubtype(ShowContactFormRemote::class.java, MessagePartTypeRemote.SHOW_CONTACT_FORM.type)
            .withSubtype(ShowSupportTicketRemote::class.java, MessagePartTypeRemote.SHOW_SUPPORT_TICKET.type)
            .withSubtype(ShowFormRemote::class.java, MessagePartTypeRemote.SHOW_FORM.type)
            .withSubtype(MessageFileRemote::class.java, MessagePartTypeRemote.FILE.type)
            .withSubtype(RequestHumanAgentRemote::class.java, MessagePartTypeRemote.REQUEST_HUMAN_AGENT.type)
            .withFallbackJsonAdapter(UnknownTypeLoggingAdapter(name = "message part type", labelKey = CONTENT_TYPE))
    )
    .add(
        PolymorphicJsonAdapterFactory.of(RichTextBlockRemote::class.java, RICH_TEXT_BLOCK_TYPE)
            .withSubtype(RichTextParagraphBlockRemote::class.java, BlockTypeRemote.RICH_TEXT_PARAGRAPH_BLOCK.type)
            .withSubtype(RichTextBulletListBlockRemote::class.java, BlockTypeRemote.RICH_TEXT_BULLET_LIST_BLOCK.type)
            .withFallbackJsonAdapter(
                UnknownTypeLoggingAdapter(name = "rich text block type", labelKey = RICH_TEXT_BLOCK_TYPE)
            )

    )
    .add(
        PolymorphicJsonAdapterFactory.of(TableCellBlockRemote::class.java, TABLE_CELL_BLOCK_TYPE)
            .withSubtype(
                RichTextParagraphBlockRemote::class.java,
                TableCellBlockTypeRemote.TABLE_CELL_PARAGRAPH_BLOCK.type
            )
            .withSubtype(
                RichTextBulletListBlockRemote::class.java,
                TableCellBlockTypeRemote.TABLE_CELL_BULLET_LIST_BLOCK.type
            )
            .withSubtype(
                TableCellImageBlockRemote::class.java,
                TableCellBlockTypeRemote.TABLE_CELL_IMAGE_BLOCK.type
            )
            .withFallbackJsonAdapter(
                UnknownTypeLoggingAdapter(name = "table cell block type", labelKey = TABLE_CELL_BLOCK_TYPE)
            )
    )
    .add(
        TableColumnAlignmentRemote::class.java,
        EnumJsonAdapter.create(TableColumnAlignmentRemote::class.java)
            .withUnknownFallback(TableColumnAlignmentRemote.UNKNOWN)
    )
    .add(
        RichTextSpanTypeRemote::class.java,
        EnumJsonAdapter.create(RichTextSpanTypeRemote::class.java)
    )
    .add(
        MessageRoleRemote::class.java,
        EnumJsonAdapter.create(MessageRoleRemote::class.java).withUnknownFallback(MessageRoleRemote.UNKNOWN)
    )
    .add(
        DeltaActionRemote::class.java,
        EnumJsonAdapter.create(DeltaActionRemote::class.java).withUnknownFallback(DeltaActionRemote.UNKNOWN)
    )
    .add(
        DeltaPartTypeRemote::class.java,
        EnumJsonAdapter.create(DeltaPartTypeRemote::class.java).withUnknownFallback(DeltaPartTypeRemote.UNKNOWN)
    )
    .add(
        DeltaBlockTypeRemote::class.java,
        EnumJsonAdapter.create(DeltaBlockTypeRemote::class.java).withUnknownFallback(DeltaBlockTypeRemote.UNKNOWN)
    )
    .add(
        HeaderAlignmentRemote::class.java,
        EnumJsonAdapter.create(HeaderAlignmentRemote::class.java).withUnknownFallback(HeaderAlignmentRemote.UNKNOWN)
    )
    .add(
        ChatFormTriggerRemote::class.java,
        EnumJsonAdapter.create(ChatFormTriggerRemote::class.java)
            .withUnknownFallback(ChatFormTriggerRemote.UNKNOWN)
    )
    .add(
        FormFieldTypeRemote::class.java,
        EnumJsonAdapter.create(FormFieldTypeRemote::class.java)
            .withUnknownFallback(FormFieldTypeRemote.UNKNOWN)
            .nullSafe()
    )
    .add(
        FormFieldConditionOperatorRemote::class.java,
        EnumJsonAdapter.create(FormFieldConditionOperatorRemote::class.java)
            .withUnknownFallback(FormFieldConditionOperatorRemote.UNKNOWN)
            .nullSafe()
    )
    .add(
        LivechatAvailabilityStatusRemote::class.java,
        EnumJsonAdapter.create(LivechatAvailabilityStatusRemote::class.java)
            .withUnknownFallback(LivechatAvailabilityStatusRemote.UNKNOWN)
            .nullSafe()
    )
    .add(
        LivechatAvailabilityReasonRemote::class.java,
        EnumJsonAdapter.create(LivechatAvailabilityReasonRemote::class.java)
            .withUnknownFallback(LivechatAvailabilityReasonRemote.UNKNOWN)
            .nullSafe()
    )
    .add(
        LivechatStateStatusRemote::class.java,
        EnumJsonAdapter.create(LivechatStateStatusRemote::class.java)
            .withUnknownFallback(LivechatStateStatusRemote.UNKNOWN)
    )
    .add(
        LivechatClosedByRemote::class.java,
        EnumJsonAdapter.create(LivechatClosedByRemote::class.java)
            .withUnknownFallback(LivechatClosedByRemote.UNKNOWN)
            .nullSafe()
    )
    .add(
        LivechatFeedbackStatusRemote::class.java,
        EnumJsonAdapter.create(LivechatFeedbackStatusRemote::class.java)
            .withUnknownFallback(LivechatFeedbackStatusRemote.UNKNOWN)
    )
