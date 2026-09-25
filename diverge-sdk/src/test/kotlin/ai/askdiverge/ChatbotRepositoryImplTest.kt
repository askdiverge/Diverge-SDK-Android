package ai.askdiverge

import app.cash.turbine.test
import ai.askdiverge.data.ChatbotRepositoryImpl
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import ai.askdiverge.data.mapper.mapToDomain
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
import ai.askdiverge.data.model.event.StreamPartEventRemote
import ai.askdiverge.data.model.event.StreamStatusEventRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.model.event.StreamStatusEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotRepositoryImplTest {

    private val testConfigRemote = ConfigRemote(
        display = DisplayRemote(
            name = "Test Bot",
            avatar = AvatarRemote(url = "https://example.com/avatar.png"),
            welcome_message = "Hello",
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

    private val remoteDataSource = mockk<ChatbotRemoteDataSource>()
    private val tokenLocalDataSource = mockk<TokenLocalDataSource> {
        every { getToken() } returns "test-token"
        every { clear() } just Runs
        every { setToken(any()) } just Runs
    }

    private val chatbotCallbacks = mockk<ChatbotCallbacks> {
        coEvery { fetchToken() } returns Result.success("fetched-token")
    }

    private val repository = ChatbotRepositoryImpl(
        chatbotCallbacks = chatbotCallbacks,
        tokenLocalDataSource = tokenLocalDataSource,
        remoteDataSource = remoteDataSource
    )

    @Nested
    @DisplayName("When initialize is called")
    inner class InitializeTest {

        @Test
        fun `reads the token then config`() = runTest {
            coEvery { remoteDataSource.getConfig() } returns Result.success(testConfigRemote)

            repository.initialize()

            verify { tokenLocalDataSource.getToken() }
            coVerify { remoteDataSource.getConfig() }
        }

        @Test
        fun `returns config on success`() = runTest {
            coEvery { remoteDataSource.getConfig() } returns Result.success(testConfigRemote)

            val result = repository.initialize()

            assertTrue(result.isSuccess)
            assertEquals(testConfigRemote.mapToDomain(), result.getOrThrow())
        }

        @Test
        fun `does not fetch a token when one is cached`() = runTest {
            coEvery { remoteDataSource.getConfig() } returns Result.success(testConfigRemote)

            repository.initialize()

            coVerify(exactly = 0) { chatbotCallbacks.fetchToken() }
        }

        @Test
        fun `fetches and caches a token when there is none`() = runTest {
            every { tokenLocalDataSource.getToken() } returns null
            coEvery { remoteDataSource.getConfig() } returns Result.success(testConfigRemote)

            val result = repository.initialize()

            assertTrue(result.isSuccess)
            // Caching before the config call is what lets the interceptor authenticate it.
            coVerifyOrder {
                chatbotCallbacks.fetchToken()
                tokenLocalDataSource.setToken("fetched-token")
                remoteDataSource.getConfig()
            }
        }

        @Test
        fun `surfaces the host's own failure when the token cannot be fetched`() = runTest {
            every { tokenLocalDataSource.getToken() } returns null
            coEvery { chatbotCallbacks.fetchToken() } returns Result.failure(IllegalStateException("Auth error"))

            val result = repository.initialize()

            assertTrue(result.isFailure)
            // The host's message is what the UI shows, so it must not be swallowed by a wrapper.
            assertEquals(
                expected = "Auth error",
                actual = result.exceptionOrNull()?.message
            )
            coVerify(exactly = 0) { remoteDataSource.getConfig() }
        }

        @Test
        fun `returns failure when config fetch fails`() = runTest {
            coEvery { remoteDataSource.getConfig() } returns Result.failure(Exception("Config error"))

            val result = repository.initialize()

            assertTrue(result.isFailure)
            assertEquals("Config error", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("When startSession is called")
    inner class StartSessionTest {

        @Test
        fun `resolves the cached token without fetching the config`() = runTest {
            val result = repository.startSession()

            assertTrue(result.isSuccess)
            verify { tokenLocalDataSource.getToken() }
            coVerify(exactly = 0) { chatbotCallbacks.fetchToken() }
            coVerify(exactly = 0) { remoteDataSource.getConfig() }
        }

        @Test
        fun `fetches and caches a token when there is none`() = runTest {
            every { tokenLocalDataSource.getToken() } returns null

            val result = repository.startSession()

            assertTrue(result.isSuccess)
            coVerify { chatbotCallbacks.fetchToken() }
            verify { tokenLocalDataSource.setToken("fetched-token") }
            coVerify(exactly = 0) { remoteDataSource.getConfig() }
        }

        @Test
        fun `surfaces the host's own failure when the token cannot be fetched`() = runTest {
            every { tokenLocalDataSource.getToken() } returns null
            coEvery { chatbotCallbacks.fetchToken() } returns Result.failure(IllegalStateException("Auth error"))

            val result = repository.startSession()

            assertTrue(result.isFailure)
            assertEquals(
                expected = "Auth error",
                actual = result.exceptionOrNull()?.message
            )
        }
    }

    @Nested
    @DisplayName("When invalidateSession is called")
    inner class InvalidateSessionTest {

        @Test
        fun `clears the token cache`() = runTest {
            repository.invalidateSession()

            verify { tokenLocalDataSource.clear() }
        }
    }

    @Nested
    @DisplayName("When renewToken is called")
    inner class RenewTokenTest {

        @Test
        fun `caches the new token from the host`() = runTest {
            coEvery { chatbotCallbacks.createNewToken() } returns Result.success("new-token")

            val result = repository.renewToken()

            assertTrue(result.isSuccess)
            verify { tokenLocalDataSource.setToken("new-token") }
        }

        @Test
        fun `keeps the current token when the host fails`() = runTest {
            coEvery { chatbotCallbacks.createNewToken() } returns Result.failure(IllegalStateException("boom"))

            val result = repository.renewToken()

            assertTrue(result.isFailure)
            verify(exactly = 0) { tokenLocalDataSource.setToken(any()) }
        }
    }

    @Nested
    @DisplayName("When deleteChatData is called")
    inner class DeleteChatDataTest {

        @Test
        fun `delegates to the host`() = runTest {
            coEvery { chatbotCallbacks.deleteChatData() } returns Result.success(Unit)

            val result = repository.deleteChatData()

            assertTrue(result.isSuccess)
            coVerify { chatbotCallbacks.deleteChatData() }
            // Clearing the cache is the caller's next step, so the session outlives this call.
            verify(exactly = 0) { tokenLocalDataSource.clear() }
        }

        @Test
        fun `keeps the session when the host fails`() = runTest {
            coEvery { chatbotCallbacks.deleteChatData() } returns Result.failure(IllegalStateException("boom"))

            val result = repository.deleteChatData()

            assertTrue(result.isFailure)
            verify(exactly = 0) { tokenLocalDataSource.clear() }
        }
    }

    @Nested
    @DisplayName("When sendMessage is called")
    inner class SendMessageTest {

        @Test
        fun `passes the message to remote data source`() = runTest {
            val messageSlot = slot<SendMessageRemote>()
            every { remoteDataSource.sendMessage(capture(messageSlot)) } returns emptyFlow()

            repository.sendMessage(
                message = "hello",
                currentPage = null
            ).test { awaitComplete() }

            val expectedMessage = SendMessageRemote(
                message = OutgoingMessageRemote(parts = listOf(OutgoingTextPartRemote(text = "hello")))
            )
            assertEquals(
                expected = expectedMessage,
                actual = messageSlot.captured
            )
        }

        @Test
        fun `maps remote events to domain events`() = runTest {
            val events = flowOf(StreamStatusEventRemote(status = "thinking"))
            every { remoteDataSource.sendMessage(any()) } returns events

            repository.sendMessage(
                message = "hello",
                currentPage = null
            ).test {
                val event = assertIs<StreamStatusEvent>(awaitItem())
                assertEquals("thinking", event.status)
                awaitComplete()
            }
        }

        @Test
        fun `filters out unmappable events`() = runTest {
            val events = flowOf(
                StreamPartEventRemote(part = null),
                StreamStatusEventRemote(status = "thinking"),
                StreamPartEventRemote(part = null)
            )
            every { remoteDataSource.sendMessage(any()) } returns events

            repository.sendMessage(
                message = "hello",
                currentPage = null
            ).test {
                assertIs<StreamStatusEvent>(awaitItem())
                awaitComplete()
            }
        }
    }
}
