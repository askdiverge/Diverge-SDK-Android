package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import com.squareup.moshi.JsonClass

/**
 * Wire `request_human_agent` message part.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/requesthumanagentmarker)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class RequestHumanAgentRemote(
    val part_id: String
) : MessagePartRemote
