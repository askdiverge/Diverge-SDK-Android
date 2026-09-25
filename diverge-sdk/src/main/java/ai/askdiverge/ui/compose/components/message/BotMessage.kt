package ai.askdiverge.ui.compose.components.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ai.askdiverge.ui.compose.components.message.product.ProductsContent
import ai.askdiverge.ui.compose.components.message.table.TableContent
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

/**
 * Renders a single part of a bot message. Text parts are wrapped in the speech bubble, while a
 * products carousel and a table are laid out full width on their own.
 */
@Composable
internal fun BotMessage(
    uiModel: MessagePartUiModel,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
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
    }
}
