package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.livechat.LivechatAgentRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityReasonRemote
import ai.askdiverge.data.model.livechat.LivechatAvailabilityStatusRemote
import ai.askdiverge.data.model.livechat.LivechatConfigRemote
import ai.askdiverge.data.model.livechat.RequestHumanAgentRemote
import ai.askdiverge.data.model.message.incoming.part.MessageFileRemote
import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatConfig
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
