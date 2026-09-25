package ai.askdiverge.domain.model.message.incoming.part.text

import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan

internal data class RichTextBulletListItem(val spans: List<RichTextSpan>)
