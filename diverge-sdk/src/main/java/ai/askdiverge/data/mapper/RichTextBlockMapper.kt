package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.text.RichTextBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListItemRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType

internal fun RichTextBlockRemote.mapToDomain(): RichTextBlock = when (this) {
    is RichTextParagraphBlockRemote -> RichTextParagraphBlock(
        spans = spans.map { it.mapToDomain() }
    )

    is RichTextBulletListBlockRemote -> RichTextBulletListBlock(
        items = items.map { it.mapToDomain() }
    )
}

internal fun RichTextBulletListItemRemote.mapToDomain(): RichTextBulletListItem = RichTextBulletListItem(
    spans = spans.map { it.mapToDomain() }
)

internal fun RichTextSpanRemote.mapToDomain(): RichTextSpan = RichTextSpan(
    text = text,
    type = type.mapToDomain(),
    url = url
)

private fun RichTextSpanTypeRemote.mapToDomain(): RichTextSpanType = when (this) {
    RichTextSpanTypeRemote.TEXT -> RichTextSpanType.TEXT
    RichTextSpanTypeRemote.BOLD -> RichTextSpanType.BOLD
    RichTextSpanTypeRemote.STRIKE -> RichTextSpanType.STRIKE
    RichTextSpanTypeRemote.LINK -> RichTextSpanType.LINK
}
