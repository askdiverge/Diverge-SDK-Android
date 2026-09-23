package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class ChatbotSnackbarPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(true, false)
}
