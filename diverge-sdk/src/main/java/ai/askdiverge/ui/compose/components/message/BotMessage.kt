package ai.askdiverge.ui.compose.components.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.askdiverge.ui.attachment.AttachmentUiCallbacks
import ai.askdiverge.ui.compose.components.livechat.HumanAgentPromptCard
import ai.askdiverge.ui.compose.components.livechat.LivechatNoteRow
import ai.askdiverge.ui.compose.components.message.attachment.MessageFileRow
import ai.askdiverge.ui.compose.components.message.attachment.MessageImageContent
import ai.askdiverge.ui.compose.components.message.form.ConversationFormCard
import ai.askdiverge.ui.compose.components.message.product.ProductsContent
import ai.askdiverge.ui.compose.components.message.table.TableContent
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.livechat.LivechatUiCallbacks
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

/**
 * Renders a single part of a bot message. Text parts are wrapped in the speech bubble, while a
 * products carousel, table, form and attachments are laid out full width on their own.
 */
@Composable
internal fun BotMessage(
    uiModel: MessagePartUiModel,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    isFormEditable: Boolean = false,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    livechatCallbacks: LivechatUiCallbacks = LivechatUiCallbacks.None,
    attachmentCallbacks: AttachmentUiCallbacks = AttachmentUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    when (uiModel) {
        is MessagePartUiModel.RichText -> BotMessageBubble(
            avatarUrl = avatarUrl,
            modifier = modifier,
            isStreaming = isStreaming
        ) {
            TextContent(
                content = uiModel,
                role = MessageRoleUiModel.BOT,
                isStreaming = isStreaming,
                onUrlClick = onUrlClick
            )
        }

        is MessagePartUiModel.Products -> ProductsContent(
            products = uiModel.products,
            modifier = modifier,
            isStreaming = isStreaming,
            onUrlClick = onUrlClick
        )

        is MessagePartUiModel.Table -> TableContent(
            content = uiModel,
            modifier = modifier,
            onUrlClick = onUrlClick
        )

        is MessagePartUiModel.Form -> ConversationFormCard(
            form = uiModel.form,
            model = formCallbacks.formModels[uiModel.form.partId],
            isEditable = isFormEditable,
            isBusy = formCallbacks.isReplyBusy,
            offersAttachments = formCallbacks.offersAttachments,
            onEnsure = formCallbacks.onEnsure,
            onValueChange = formCallbacks.onValueChange,
            onSubmit = formCallbacks.onSubmit,
            onReload = formCallbacks.onReload,
            onPickFile = formCallbacks.onPickFile,
            onRemoveFile = formCallbacks.onRemoveFile,
            onPickTicketAttachment = formCallbacks.onPickTicketAttachment,
            onRemoveTicketAttachment = formCallbacks.onRemoveTicketAttachment,
            modifier = modifier
        )

        is MessagePartUiModel.RequestHumanAgent -> HumanAgentPromptCard(
            enabled = isFormEditable && livechatCallbacks.canAcceptHumanAgent(uiModel.partId),
            onAccept = { livechatCallbacks.onAcceptHumanAgent(uiModel.partId) },
            onDecline = { livechatCallbacks.onDeclineHumanAgent(uiModel.partId) },
            modifier = modifier
        )

        is MessagePartUiModel.LivechatNote -> LivechatNoteRow(
            note = uiModel,
            modifier = modifier
        )

        is MessagePartUiModel.File -> MessageFileRow(
            file = uiModel,
            onOpen = attachmentCallbacks.onOpen,
            modifier = modifier
        )

        is MessagePartUiModel.Image -> MessageImageContent(
            image = uiModel,
            onOpen = attachmentCallbacks.onOpen,
            modifier = modifier
        )
    }
}
