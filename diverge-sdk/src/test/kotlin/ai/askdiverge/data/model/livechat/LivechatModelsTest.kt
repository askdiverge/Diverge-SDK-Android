package ai.askdiverge.data.model.livechat

import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.RequestHumanAgent
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Pins livechat config, agent messages, file parts and handover markers against the Chatbot API.
 */
class LivechatModelsTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val configAdapter = moshi.adapter(ConfigRemote::class.java)
    private val livechatAdapter = moshi.adapter(LivechatConfigRemote::class.java)
    private val messageAdapter = moshi.adapter(MessageRemote::class.java)
    private val partAdapter = moshi.adapter(MessagePartRemote::class.java)

    @Nested
    @DisplayName("LivechatConfig")
    inner class ConfigBlock {

        @Test
        fun `config defaults livechat when the block is absent`() {
            val config = decodeConfig(extra = "")

            assertEquals(LivechatConfig.serverDefaults, config.livechat)
        }

        @Test
        fun `an enabled live config decodes`() {
            val settings = requireNotNull(
                livechatAdapter.fromJson(
                    """
                    {
                      "enabled": true,
                      "configured": true,
                      "availability_status": "live",
                      "availability_reason": "always_on",
                      "show_livechat_logo": true,
                      "attachments_enabled": true,
                      "max_attachment_size_bytes": 5242880
                    }
                    """.trimIndent()
                )
            ).mapToDomain()

            assertTrue(settings.enabled)
            assertEquals(LivechatConfig.AvailabilityStatus.LIVE, settings.availabilityStatus)
            assertEquals(LivechatConfig.AvailabilityReason.ALWAYS_ON, settings.availabilityReason)
            assertTrue(settings.attachmentsEnabled)
            assertEquals(5_242_880, settings.maxAttachmentSizeBytes)
        }

        @Test
        fun `availability values the SDK does not know decode as unknown`() {
            val settings = requireNotNull(
                livechatAdapter.fromJson(
                    """
                    {
                      "enabled": true,
                      "configured": true,
                      "availability_status": "busy",
                      "availability_reason": "holiday"
                    }
                    """.trimIndent()
                )
            ).mapToDomain()

            assertEquals(LivechatConfig.AvailabilityStatus.UNKNOWN, settings.availabilityStatus)
            assertEquals(LivechatConfig.AvailabilityReason.UNKNOWN, settings.availabilityReason)
        }

        @Test
        fun `missing attachments_enabled fails closed and missing max size uses 5 MiB`() {
            val settings = requireNotNull(
                livechatAdapter.fromJson(
                    """
                    {
                      "enabled": true,
                      "configured": true,
                      "availability_status": "live",
                      "availability_reason": "always_on",
                      "show_livechat_logo": true
                    }
                    """.trimIndent()
                )
            ).mapToDomain()

            assertFalse(settings.attachmentsEnabled)
            assertEquals(5_242_880, settings.maxAttachmentSizeBytes)
        }
    }

    @Nested
    @DisplayName("Parts and messages")
    inner class PartsAndMessages {

        @Test
        fun `request_human_agent marker decodes`() {
            val part = requireNotNull(
                partAdapter.fromJson(
                    """{ "type": "request_human_agent", "part_id": "part_human_1" }"""
                )
            ).mapToDomain()

            val marker = assertIs<RequestHumanAgent>(part)
            assertEquals("part_human_1", marker.partId)
        }

        @Test
        fun `a history message from an agent carries the agent identity`() {
            val message = requireNotNull(
                messageAdapter.fromJson(
                    """
                    {
                      "message_id": "m_2",
                      "role": "agent",
                      "agent": {
                        "agent_id": "00000000-0000-4000-8000-0000000000a1",
                        "display_name": "Alice",
                        "avatar_url": null
                      },
                      "parts": []
                    }
                    """.trimIndent()
                )
            ).mapToDomain()

            assertEquals(MessageRole.AGENT, message.role)
            assertEquals("Alice", message.agent?.displayName)
            assertNull(message.agent?.avatarUrl)
        }

        @Test
        fun `a file part decodes its download URL as sent`() {
            val part = requireNotNull(
                partAdapter.fromJson(
                    """{ "type": "file", "filename": "a.png", "url": "DATA:image/png;base64,QQ==" }"""
                )
            ).mapToDomain()

            val file = assertIs<MessageFile>(part)
            assertEquals("a.png", file.filename)
            assertEquals("DATA:image/png;base64,QQ==", file.url)
        }

        @Test
        fun `a malformed file part is dropped and the rest of its message still decodes`() {
            val message = requireNotNull(
                messageAdapter.fromJson(
                    """
                    {
                      "message_id": "m_3",
                      "role": "agent",
                      "agent": { "agent_id": "a1", "display_name": "Alice" },
                      "parts": [
                        { "type": "file", "filename": "gone.pdf", "url": null },
                        { "type": "request_human_agent", "part_id": "part_human_2" }
                      ]
                    }
                    """.trimIndent()
                )
            ).mapToDomain()

            val marker = assertIs<RequestHumanAgent>(message.parts.single())
            assertEquals("part_human_2", marker.partId)
            assertEquals("Alice", message.agent?.displayName)
        }
    }

    private fun decodeConfig(extra: String) = requireNotNull(
        configAdapter.fromJson(
            """
            {
              "display": {
                "name": "Bot",
                "avatar": { "url": null },
                "welcome_message": null,
                "subtitle": null,
                "privacy_policy_url": "https://example.com/privacy"
              },
              "theme": {
                "brand": { "primary_color": "#000000" },
                "surface": { "background_color": "#FFFFFF", "muted_text_color": "#666666" },
                "header": {
                  "alignment": "center",
                  "logo": { "url": "https://example.com/logo.png" },
                  "button": { "background_color": "#EEE", "icon_color": "#111" }
                },
                "messages": {
                  "assistant": {
                    "background_color": "#F3F4F6",
                    "text_color": "#111",
                    "thinking_border_gradient": ["#FFF", "#000"]
                  },
                  "user": { "background_color": "#4F46E5", "text_color": "#FFF" }
                },
                "input": {
                  "text_color": "#111",
                  "placeholder_color": "#999",
                  "send_button": {}
                },
                "product_card": { "discount_price_color": "#C00" },
                "font": {
                  "android": { "asset_url": "https://example.com/font.ttf" }
                }
              }
            $extra
            }
            """.trimIndent()
        )
    ).mapToDomain()
}
