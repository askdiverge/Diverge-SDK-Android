package ai.askdiverge.ui.model.message.part.text

import androidx.compose.runtime.Immutable

@Immutable
internal data class BulletListItemUiModel(val spans: List<RichTextSpanUiModel>)
