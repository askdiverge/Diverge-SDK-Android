package ai.askdiverge.ui.model.message.part

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import androidx.compose.runtime.Immutable

internal sealed class MessagePartUiModel {
    @Immutable
    data class RichText(val blocks: List<RichTextBlockUiModel>) : MessagePartUiModel()

    @Immutable
    data class Products(val products: List<ProductCardUiModel>) : MessagePartUiModel()

    @Immutable
    data class Table(
        val caption: String?,
        val headers: List<TableCellUiModel>,
        val rows: List<List<TableCellUiModel>>
    ) : MessagePartUiModel()

    @Immutable
    data class Form(val form: ConversationForm) : MessagePartUiModel()

    /** A `request_human_agent` marker rendered as a talk-to-a-person card. */
    @Immutable
    data class RequestHumanAgent(val partId: String) : MessagePartUiModel()

    /** A `request_image_upload` marker rendered as an Add-photo card. */
    @Immutable
    data class RequestImageUpload(
        val partId: String,
        val acceptedTypes: List<String>,
        val maxSizeBytes: Int
    ) : MessagePartUiModel()

    /** A session-local livechat boundary note (queued / agent joined / ended). */
    @Immutable
    data class LivechatNote(
        val kind: LivechatNoteKind,
        /** Set for [LivechatNoteKind.AGENT_JOINED]. */
        val agentDisplayName: String? = null
    ) : MessagePartUiModel()

    /** A file attachment row; [displayUrl] is absolute (or `data:`) for opening. */
    @Immutable
    data class File(
        val filename: String,
        val displayUrl: String,
        val attachmentId: String?,
        val mimeType: String?,
        val sizeBytes: Int?,
        val urlExpiresAt: String?
    ) : MessagePartUiModel()

    /** An image attachment tile; [displayUrl] / [thumbnailUrl] are loadable absolute URLs. */
    @Immutable
    data class Image(
        val displayUrl: String,
        val thumbnailUrl: String?,
        val attachmentId: String?,
        val mimeType: String?,
        val sizeBytes: Int?,
        val urlExpiresAt: String?,
        val caption: String?
    ) : MessagePartUiModel()
}

internal enum class LivechatNoteKind {
    QUEUED,
    AGENT_JOINED,
    ENDED,
    ENDED_FOR_INACTIVITY
}
