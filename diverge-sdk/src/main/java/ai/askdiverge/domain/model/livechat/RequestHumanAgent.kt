package ai.askdiverge.domain.model.livechat

import ai.askdiverge.domain.model.message.incoming.part.MessagePart

/**
 * Instructs the client to show a handoff affordance for a human agent. Clients may pass
 * [partId] to `POST /livechat/handover`. Arrives whole, as a `part` event, in history or in
 * `done`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/requesthumanagentmarker)
 */
internal data class RequestHumanAgent(
    /** The marker's `part_id`, echoed to `POST /livechat/handover`. */
    val partId: String
) : MessagePart
