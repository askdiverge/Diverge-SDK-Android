package ai.askdiverge.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ChatbotAttachments
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.ChatbotRating
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.ui.ChatbotViewModel
import ai.askdiverge.ui.attachment.AttachmentUiCallbacks
import ai.askdiverge.ui.compose.components.ChatbotContent
import ai.askdiverge.ui.compose.theme.ChatbotTheme
import ai.askdiverge.ui.compose.theme.screenBackground

private val SHEET_CORNER_RADIUS = 28.dp

/**
 * The chatbot conversation presented as a modal bottom sheet.
 *
 * @param chatbotCallbacks the host's implementation of the actions the chatbot delegates to it.
 * @param onDismiss called when the sheet has been dismissed (after any conversation-rating prompt).
 * @param modifier applied to the sheet.
 * @param currentPage the page the sheet was opened from, sent along with each message.
 * @param fontFamily the font the chat draws in. The host's own is used when null, which needs a
 * Material 3 theme in scope; pass one to set it from a Material 2 or unthemed host.
 * @param onUrlClick handles links in messages. Links open in the platform handler when null.
 * @param environment which Diverge backend the chat talks to. Defaults to
 * [DivergeEnvironment.PRODUCTION].
 * @param attachments whether the system photo picker is offered: in the composer, to send photos
 * to the assistant or a live agent, on the card shown when the assistant asks for a photo, and in
 * form file fields and support-ticket photos. Defaults to [ChatbotAttachments.DISABLED], which
 * also hides that card; pass [ChatbotAttachments.PHOTO_LIBRARY] to offer photo attach.
 * Read once, when the chat's ViewModel is created: a different value passed while the sheet's
 * ViewModel is alive has no effect.
 * @param rating whether dismissing the sheet may offer a conversation rating after the visitor has
 * sent a message. Defaults to [ChatbotRating.ENABLED]; pass [ChatbotRating.DISABLED] to call
 * [onDismiss] immediately on dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotBottomSheet(
    chatbotCallbacks: ChatbotCallbacks,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentPage: String? = null,
    fontFamily: FontFamily? = null,
    onUrlClick: ((String) -> Unit)? = null,
    environment: DivergeEnvironment = DivergeEnvironment.PRODUCTION,
    attachments: ChatbotAttachments = ChatbotAttachments.DISABLED,
    rating: ChatbotRating = ChatbotRating.ENABLED
) {
    val viewModel: ChatbotViewModel = viewModel(
        factory = ChatbotViewModel.factory(
            chatbotCallbacks = chatbotCallbacks,
            environment = environment,
            currentPage = currentPage,
            attachments = attachments,
            rating = rating
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyPagingItems = viewModel.historyMessages.collectAsLazyPagingItems()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue != SheetValue.Hidden) true
            else viewModel.onSheetDismissRequested()
        }
    )

    LaunchedEffect(Unit) { viewModel.onSheetOpened() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // Saved across a configuration change, so a rating dismiss sent while the sheet is recreated
    // still closes it; a sheet the host opens again starts false and drops one left from before.
    var collectsDismiss by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        if (!collectsDismiss) {
            viewModel.dropPendingDismiss()
            collectsDismiss = true
        }
        viewModel.dismissEvents.collect { currentOnDismiss() }
    }
    // Livechat polls only while the chat is on screen and the host is started.
    LifecycleStartEffect(viewModel) {
        viewModel.onChatVisible()
        onStopOrDispose { viewModel.onChatHidden() }
    }

    ChatbotTheme(
        theme = uiState.theme,
        fontFamily = fontFamily
    ) {
        ModalBottomSheet(
            // Material3 1.4 asks confirmValueChange before a swipe, scrim tap or back press hides
            // the sheet, and calls this even when that vetoed the hide. A hidden sheet closes; a
            // visible one asks the gate, which returns false while the rating prompt is open.
            onDismissRequest = {
                if (!sheetState.isVisible || viewModel.onSheetDismissRequested()) {
                    onDismiss()
                }
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = SHEET_CORNER_RADIUS,
                topEnd = SHEET_CORNER_RADIUS
            ),
            containerColor = MaterialTheme.colorScheme.screenBackground,
            tonalElevation = 0.dp,
            // The top inset is already applied by statusBarsPadding() on the sheet itself.
            contentWindowInsets = {
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            },
            modifier = modifier.statusBarsPadding()
        ) {
            ChatbotContent(
                uiState = uiState,
                historyPagingItems = historyPagingItems,
                hasNavigateBackIcon = false,
                onInputChange = viewModel::onInputChanged,
                onPostMessage = viewModel::postMessage,
                onScrolledToMessage = viewModel::onScrolledToMessage,
                onSnackbarConsumed = viewModel::onSnackbarConsumed,
                onSheetRequested = viewModel::onSheetRequested,
                onSheetDismissed = viewModel::onSheetDismissed,
                onNavigateBack = {
                    if (viewModel.onSheetDismissRequested()) {
                        onDismiss()
                    }
                },
                onRetry = viewModel::retry,
                onHistoryLoadFailed = viewModel::onHistoryLoadFailed,
                modifier = Modifier.fillMaxSize(),
                onDeleteChatData = viewModel::deleteChatData,
                onResetConversation = viewModel::resetConversation,
                onFormEnsure = viewModel::onFormEnsure,
                onFormValueChange = viewModel::onFormValueChanged,
                onFormSubmit = viewModel::onFormSubmit,
                onFormReload = viewModel::onFormReload,
                offersAttachments = viewModel.offersAttachments,
                onFormPhotoPicked = viewModel::onFormPhotoPicked,
                onFormFileRemoved = viewModel::onFormFileRemoved,
                onTicketAttachmentRemoved = viewModel::onTicketAttachmentRemoved,
                onLivechatControlClick = viewModel::onLivechatControlClick,
                onConfirmLeaveLivechat = viewModel::onConfirmLeaveLivechat,
                onDismissLeaveLivechat = viewModel::onDismissLeaveLivechat,
                onAcceptHumanAgent = viewModel::onAcceptHumanAgent,
                onDeclineHumanAgent = viewModel::onDeclineHumanAgent,
                attachmentCallbacks = AttachmentUiCallbacks(
                    freshUrl = viewModel::freshAttachmentUrl,
                    onOpen = viewModel::onAttachmentOpen
                ),
                openUrlEvents = viewModel.openUrlEvents,
                onOpenUrlFailed = viewModel::onOpenUrlFailed,
                onWaitingFormValueChange = viewModel::onWaitingFormValueChanged,
                onWaitingFormSave = viewModel::onWaitingFormSave,
                onLivechatRatingScore = viewModel::onLivechatRatingScore,
                onLivechatRatingCommentChange = viewModel::onLivechatRatingCommentChanged,
                onLivechatRatingSend = viewModel::onLivechatRatingSend,
                onLivechatRatingSkip = viewModel::onLivechatRatingSkip,
                onConversationRatingScore = viewModel::onConversationRatingScore,
                onConversationRatingCommentChange = viewModel::onConversationRatingCommentChanged,
                onConversationRatingSend = viewModel::onConversationRatingSend,
                onConversationRatingSkip = viewModel::onConversationRatingSkip,
                onConversationRatingCancel = viewModel::onConversationRatingCancel,
                canAttach = viewModel.canAttachConsideringLivechat(),
                onComposerPhotoPicked = viewModel::onComposerPhotoPicked,
                onComposerPhotoPickDismissed = viewModel::onComposerPhotoPickDismissed,
                onRemovePendingAttachment = viewModel::onRemovePendingAttachment,
                onImageUploadPromptAddPhoto = viewModel::onImageUploadPromptAddPhoto,
                onUrlClick = onUrlClick
            )
        }
    }
}
