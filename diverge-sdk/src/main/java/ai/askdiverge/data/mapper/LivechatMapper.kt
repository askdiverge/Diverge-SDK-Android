package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.livechat.LivechatAgentRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityReasonRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityStatusRemote
import ai.askdiverge.data.model.livechat.LivechatClientContextRemote
import ai.askdiverge.data.model.livechat.LivechatClosedByRemote
import ai.askdiverge.data.model.livechat.LivechatConfigRemote
import ai.askdiverge.data.model.livechat.LivechatFeedbackRemote
import ai.askdiverge.data.model.livechat.LivechatFeedbackStatusRemote
import ai.askdiverge.data.model.livechat.LivechatHandoverRequestRemote
import ai.askdiverge.data.model.livechat.LivechatHandoverSourceRemote
import ai.askdiverge.data.model.livechat.LivechatMessageRemote
import ai.askdiverge.data.model.livechat.LivechatSessionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatStateRemote
import ai.askdiverge.data.model.livechat.LivechatStateStatusRemote
import ai.askdiverge.data.model.livechat.LivechatSyncRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessagePayloadRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageRequestRemote
import ai.askdiverge.data.model.livechat.RequestHumanAgentRemote
import ai.askdiverge.data.model.message.OutgoingMessageContextRemote
import ai.askdiverge.data.model.message.incoming.part.MessageFileRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingImagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
import ai.askdiverge.domain.model.livechat.RequestHumanAgent
import ai.askdiverge.domain.model.message.incoming.part.MessageFile


internal fun LivechatConfigRemote?.mapToDomain(): LivechatConfig {
    val defaults = LivechatConfig.serverDefaults
    if (this == null) return defaults
    return LivechatConfig(
        enabled = enabled ?: defaults.enabled,
        configured = configured ?: defaults.configured,
        availabilityStatus = availability_status.mapToDomain(),
        availabilityReason = availability_reason.mapToDomain(),
        showLivechatLogo = show_livechat_logo ?: defaults.showLivechatLogo,
        attachmentsEnabled = attachments_enabled ?: defaults.attachmentsEnabled,
        maxAttachmentSizeBytes = max_attachment_size_bytes
            ?: defaults.maxAttachmentSizeBytes
    )
}

internal fun LivechatAgentRemote.mapToDomain() = LivechatAgent(
    agentId = agent_id,
    displayName = display_name,
    avatarUrl = avatar_url
)

internal fun MessageFileRemote.mapToDomain() = MessageFile(
    filename = filename,
    url = url,
    attachmentId = attachment_id,
    mimeType = mime_type,
    sizeBytes = size_bytes,
    urlExpiresAt = url_expires_at
)

internal fun RequestHumanAgentRemote.mapToDomain() = RequestHumanAgent(partId = part_id)

internal fun LivechatSyncRemote.mapToDomain() = LivechatSync(
    state = state.mapToDomain(),
    messages = messages.map { it.mapToDomain() },
    hasMore = has_more,
    syncCursor = sync_cursor
)

internal fun LivechatStateRemote.mapToDomain() = LivechatState(
    status = status.mapToDomain(),
    activeAgent = active_agent?.mapToDomain(),
    isAgentTyping = is_agent_typing,
    agentJoinedAt = agent_joined_at,
    closedBy = closed_by.mapToDomain(),
    closedByAgentId = closed_by_agent_id,
    closeReason = close_reason,
    feedback = feedback.mapToDomain(),
    stateVersion = state_version
)

internal fun LivechatMessageRemote.mapToDomain() = LivechatMessage(
    messageId = message_id,
    role = role.mapToDomain(),
    agent = agent?.mapToDomain(),
    parts = parts.mapNotNull { it.mapToDomain() },
    createdAt = created_at,
    sequenceNumber = sequence_number
)

internal fun LivechatStateStatusRemote.mapToDomain(): LivechatState.Status = when (this) {
    LivechatStateStatusRemote.INACTIVE -> LivechatState.Status.INACTIVE
    LivechatStateStatusRemote.WAITING -> LivechatState.Status.WAITING
    LivechatStateStatusRemote.ACTIVE -> LivechatState.Status.ACTIVE
    LivechatStateStatusRemote.CLOSED -> LivechatState.Status.CLOSED
    LivechatStateStatusRemote.UNKNOWN -> LivechatState.Status.UNKNOWN
}

internal fun LivechatSessionResponseRemote.mapToDomain(): LivechatState.Status = status.mapToDomain()

internal fun LivechatHandoverRequest.mapToRemote() = LivechatHandoverRequestRemote(
    platform = platform,
    source = when (source) {
        LivechatHandoverRequest.Source.MANUAL_BUTTON -> LivechatHandoverSourceRemote.MANUAL_BUTTON
        LivechatHandoverRequest.Source.ASSISTANT_MARKER -> LivechatHandoverSourceRemote.ASSISTANT_MARKER
    },
    part_id = partId,
    client_context = clientContext?.mapToRemote()
)

internal fun LivechatVisitorMessageRequest.mapToRemote() = LivechatVisitorMessageRequestRemote(
    message = LivechatVisitorMessagePayloadRemote(
        parts = buildOutgoingParts(text = text, images = images)
    ),
    context = currentPage?.let { OutgoingMessageContextRemote(page = it) },
    client_operation_id = clientOperationId
)

internal fun buildOutgoingParts(
    text: String,
    images: List<OutgoingImageInput>
): List<OutgoingMessagePartRemote> {
    val parts = mutableListOf<OutgoingMessagePartRemote>()
    // The backend rejects a text part that is blank once trimmed, so whitespace sends no text.
    if (text.isNotBlank()) {
        parts += OutgoingTextPartRemote(text = text)
    }
    for (image in images) {
        parts += OutgoingImagePartRemote(
            data = image.dataBase64,
            mime = image.mimeType,
            filename = image.filename
        )
    }
    return parts
}

private fun LivechatClientContext.mapToRemote() = LivechatClientContextRemote(
    browser = browser,
    browser_language = browserLanguage,
    browser_version = browserVersion,
    current_page_url = currentPageUrl,
    language = language,
    os = os
)

private fun LivechatFeedbackRemote.mapToDomain() = LivechatState.Feedback(
    status = when (status) {
        LivechatFeedbackStatusRemote.NOT_AVAILABLE -> LivechatState.Feedback.Status.NOT_AVAILABLE
        LivechatFeedbackStatusRemote.PENDING -> LivechatState.Feedback.Status.PENDING
        LivechatFeedbackStatusRemote.SUBMITTED -> LivechatState.Feedback.Status.SUBMITTED
        LivechatFeedbackStatusRemote.UNKNOWN -> LivechatState.Feedback.Status.UNKNOWN
    }
)

private fun LivechatClosedByRemote?.mapToDomain(): LivechatState.ClosedBy? = when (this) {
    LivechatClosedByRemote.CUSTOMER -> LivechatState.ClosedBy.CUSTOMER
    LivechatClosedByRemote.AGENT -> LivechatState.ClosedBy.AGENT
    LivechatClosedByRemote.UNKNOWN -> LivechatState.ClosedBy.UNKNOWN
    null -> null
}

private fun LivechatAvailabilityStatusRemote?.mapToDomain(): LivechatConfig.AvailabilityStatus =
    when (this) {
        LivechatAvailabilityStatusRemote.LIVE -> LivechatConfig.AvailabilityStatus.LIVE
        LivechatAvailabilityStatusRemote.OFFLINE -> LivechatConfig.AvailabilityStatus.OFFLINE
        LivechatAvailabilityStatusRemote.UNKNOWN -> LivechatConfig.AvailabilityStatus.UNKNOWN
        null -> LivechatConfig.serverDefaults.availabilityStatus
    }

private fun LivechatAvailabilityReasonRemote?.mapToDomain(): LivechatConfig.AvailabilityReason =
    when (this) {
        LivechatAvailabilityReasonRemote.DISABLED -> LivechatConfig.AvailabilityReason.DISABLED
        LivechatAvailabilityReasonRemote.MANUAL_LIVE -> LivechatConfig.AvailabilityReason.MANUAL_LIVE
        LivechatAvailabilityReasonRemote.MANUAL_OFFLINE ->
            LivechatConfig.AvailabilityReason.MANUAL_OFFLINE
        LivechatAvailabilityReasonRemote.ALWAYS_ON -> LivechatConfig.AvailabilityReason.ALWAYS_ON
        LivechatAvailabilityReasonRemote.WITHIN_WORKING_HOURS ->
            LivechatConfig.AvailabilityReason.WITHIN_WORKING_HOURS
        LivechatAvailabilityReasonRemote.OUTSIDE_WORKING_HOURS ->
            LivechatConfig.AvailabilityReason.OUTSIDE_WORKING_HOURS
        LivechatAvailabilityReasonRemote.UNKNOWN -> LivechatConfig.AvailabilityReason.UNKNOWN
        null -> LivechatConfig.serverDefaults.availabilityReason
    }
