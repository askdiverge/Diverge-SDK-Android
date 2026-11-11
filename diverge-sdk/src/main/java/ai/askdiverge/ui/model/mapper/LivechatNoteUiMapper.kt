package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.livechat.LivechatNote
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.LivechatNoteKind
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import java.util.UUID

internal fun LivechatNote.toBubble(): ChatBubbleUiModel {
    val (kind, agentName) = when (val noteKind = this.kind) {
        LivechatNote.Kind.Queued -> LivechatNoteKind.QUEUED to null
        is LivechatNote.Kind.AgentJoined ->
            LivechatNoteKind.AGENT_JOINED to noteKind.displayName.trim().takeIf { it.isNotEmpty() }
        LivechatNote.Kind.Ended -> LivechatNoteKind.ENDED to null
        LivechatNote.Kind.EndedForInactivity -> LivechatNoteKind.ENDED_FOR_INACTIVITY to null
    }
    return ChatBubbleUiModel(
        role = MessageRoleUiModel.BOT,
        part = MessagePartUiModel.LivechatNote(
            kind = kind,
            agentDisplayName = agentName
        ),
        id = UUID.randomUUID().toString()
    )
}
