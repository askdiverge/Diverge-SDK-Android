package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.config.theme.header.HeaderAlignmentRemote
import ai.askdiverge.data.model.event.delta.DeltaActionRemote
import ai.askdiverge.data.model.event.delta.DeltaBlockTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaPartTypeRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.BlockTypeRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
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

internal fun Moshi.Builder.buildConversationModels(): Moshi.Builder = add(
    SkipUnknownInListAdapterFactory(
        targetTypes = setOf(
            MessagePartRemote::class.java,
            RichTextBlockRemote::class.java,
            TableCellBlockRemote::class.java
        )
    )
)
    .add(
        PolymorphicJsonAdapterFactory
            .of(OutgoingMessagePartRemote::class.java, MESSAGE_PART_TYPE)
            .withSubtype(OutgoingTextPartRemote::class.java, OutgoingMessageTypeRemote.TEXT.type)
    )
    .add(
        PolymorphicJsonAdapterFactory.of(MessagePartRemote::class.java, CONTENT_TYPE)
            .withSubtype(RichTextPartRemote::class.java, MessagePartTypeRemote.RICH_TEXT.type)
            .withSubtype(ImagePartRemote::class.java, MessagePartTypeRemote.IMAGE.type)
            .withSubtype(ProductPartRemote::class.java, MessagePartTypeRemote.PRODUCTS.type)
            .withSubtype(TablePartRemote::class.java, MessagePartTypeRemote.TABLE.type)
            .withDefaultValue(null)
    )
    .add(
        PolymorphicJsonAdapterFactory.of(RichTextBlockRemote::class.java, RICH_TEXT_BLOCK_TYPE)
            .withSubtype(RichTextParagraphBlockRemote::class.java, BlockTypeRemote.RICH_TEXT_PARAGRAPH_BLOCK.type)
            .withSubtype(RichTextBulletListBlockRemote::class.java, BlockTypeRemote.RICH_TEXT_BULLET_LIST_BLOCK.type)
            .withDefaultValue(null)
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
            .withDefaultValue(null)
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
