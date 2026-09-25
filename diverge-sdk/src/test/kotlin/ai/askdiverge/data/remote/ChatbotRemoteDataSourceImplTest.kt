package ai.askdiverge.data.remote

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.config.display.AvatarRemote
import ai.askdiverge.data.model.config.display.DisplayRemote
import ai.askdiverge.data.model.config.theme.BrandRemote
import ai.askdiverge.data.model.config.theme.ThemeProductCardRemote
import ai.askdiverge.data.model.config.theme.ThemeRemote
import ai.askdiverge.data.model.config.theme.ThemeSurfaceRemote
import ai.askdiverge.data.model.config.theme.font.ThemeAndroidFontRemote
import ai.askdiverge.data.model.config.theme.font.ThemeFontRemote
import ai.askdiverge.data.model.config.theme.header.HeaderAlignmentRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderButtonRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderRemote
import ai.askdiverge.data.model.config.theme.header.ThemeLogoRemote
import ai.askdiverge.data.model.config.theme.input.ThemeInputRemote
import ai.askdiverge.data.model.config.theme.input.ThemeSendButtonRemote
import ai.askdiverge.data.model.config.theme.messages.AssistantMessageStyleRemote
import ai.askdiverge.data.model.config.theme.messages.ThemeMessagesRemote
import ai.askdiverge.data.model.config.theme.messages.UserMessageStyleRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSourceImpl
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import retrofit2.Response

class ChatbotRemoteDataSourceImplTest {

    private val service = mockk<ChatbotService>()
    private val sseService = mockk<ChatbotStreamService>()
    private val dataSource = ChatbotRemoteDataSourceImpl(service = service, streamService = sseService)

    @Nested
    @DisplayName("When fetchConfig is called")
    inner class FetchConfigTest {

        @Test
        fun `returns success with config on successful response`() = runTest {
            val expected = ConfigRemote(
                display = DisplayRemote(
                    name = "Bot",
                    avatar = AvatarRemote(url = "url"),
                    welcome_message = "Hi",
                    subtitle = null,
                    privacy_policy_url = "https://example.com/privacy-policy"
                ),
                theme = ThemeRemote(
                    brand = BrandRemote(primary_color = "#221d25"),
                    surface = ThemeSurfaceRemote(
                        background_gradient_color = null,
                        background_color = null,
                        muted_text_color = null
                    ),
                    header = ThemeHeaderRemote(
                        alignment = HeaderAlignmentRemote.LEFT,
                        logo = ThemeLogoRemote(url = null),
                        button = ThemeHeaderButtonRemote(background_color = null, icon_color = null)
                    ),
                    messages = ThemeMessagesRemote(
                        assistant = AssistantMessageStyleRemote(
                            background_color = "#f5f5f5",
                            text_color = "#221d25",
                            border_color = null,
                            thinking_border_gradient = emptyList()
                        ),
                        user = UserMessageStyleRemote(
                            background_color = "#4c4d52",
                            text_color = "#fbf6f1",
                            border_color = null
                        )
                    ),
                    input = ThemeInputRemote(
                        text_color = null,
                        placeholder_color = null,
                        background_color = null,
                        border_color = null,
                        send_button = ThemeSendButtonRemote(icon_color = null)
                    ),
                    product_card = ThemeProductCardRemote(discount_price_color = null),
                    font = ThemeFontRemote(
                        android = ThemeAndroidFontRemote(asset_url = "https://example.com/font.ttc")
                    )
                )
            )
            coEvery { service.config() } returns Response.success(expected)

            val result = dataSource.getConfig()

            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `returns failure on unsuccessful response`() = runTest {
            coEvery { service.config() } returns Response.error(500, ResponseBody.EMPTY)

            val result = dataSource.getConfig()

            assertTrue(result.isFailure)
            assertEquals("Config error", result.exceptionOrNull()?.message)
        }

        @Test
        fun `returns failure when response body is null`() = runTest {
            coEvery { service.config() } returns Response.success(null)

            val result = dataSource.getConfig()

            assertTrue(result.isFailure)
            assertEquals("Config error", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("When getMessageHistory is called")
    inner class GetMessageHistoryTest {

        @Test
        fun `returns success with message page on successful response`() = runTest {
            val expected = MessagePageRemote(
                has_more = false,
                messages = emptyList(),
                next_cursor = ""
            )
            coEvery {
                service.getMessageHistory(
                    cursor = null,
                    limit = 10
                )
            } returns Response.success(expected)

            val result = dataSource.getMessageHistory(
                cursor = null,
                pageSize = 10
            )

            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `returns failure on unsuccessful response`() = runTest {
            coEvery {
                service.getMessageHistory(
                    cursor = null,
                    limit = 10
                )
            } returns Response.error(500, ResponseBody.EMPTY)

            val result = dataSource.getMessageHistory(
                cursor = null,
                pageSize = 10
            )

            assertTrue(result.isFailure)
            assertEquals("Could not get messages", result.exceptionOrNull()?.message)
        }

        @Test
        fun `returns failure when response body is null`() = runTest {
            coEvery {
                service.getMessageHistory(
                    cursor = null,
                    limit = 10
                )
            } returns Response.success(null)

            val result = dataSource.getMessageHistory(
                cursor = null,
                pageSize = 10
            )

            assertTrue(result.isFailure)
            assertEquals("Could not get messages", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("When sendMessage is called")
    inner class SendMessageTest {

        @Test
        fun `delegates to sseService`() {
            val message = SendMessageRemote(
                message = OutgoingMessageRemote(parts = listOf(OutgoingTextPartRemote(text = "hi")))
            )
            every { sseService.sendMessage(message) } returns flowOf()

            dataSource.sendMessage(message)

            verify { sseService.sendMessage(message) }
        }
    }
}
