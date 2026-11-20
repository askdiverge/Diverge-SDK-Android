package ai.askdiverge

import app.cash.turbine.test
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.ChatbotRepositoryImpl
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.banner.ChatBannerListRemote
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
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.ContactFormActionRemote
import ai.askdiverge.data.model.form.FormFieldRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.MessageFileRemote
import ai.askdiverge.data.model.livechat.LivechatFeedbackRequestRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingImagePartRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.model.rating.ConversationRatingRequestRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.event.StreamStatusEvent
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import androidx.paging.PagingConfig
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        remoteDataSource = remoteDataSource,
        environment = DivergeEnvironment.PRODUCTION
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

        @Test
        fun `keeps the cached token when config fails for another reason`() = runTest {
            coEvery { remoteDataSource.getConfig() } returns Result.failure(Exception("Config error"))

            repository.initialize()

            verify(exactly = 0) { tokenLocalDataSource.clear() }
            coVerify(exactly = 0) { chatbotCallbacks.fetchToken() }
            coVerify(exactly = 1) { remoteDataSource.getConfig() }
        }

        @Test
        fun `replaces a token config rejects with the host's and tries once more`() = runTest {
            val tokens = cachedTokens(initial = "rejected-token")
            coEvery { remoteDataSource.getConfig() } returnsMany listOf(
                Result.failure(ChatbotException.SessionExpired()),
                Result.success(testConfigRemote)
            )

            val result = repository.initialize()

            assertEquals(testConfigRemote.mapToDomain(), result.getOrThrow())
            assertEquals("fetched-token", tokens.current)
            coVerifyOrder {
                remoteDataSource.getConfig()
                tokenLocalDataSource.clear()
                chatbotCallbacks.fetchToken()
                tokenLocalDataSource.setToken("fetched-token")
                remoteDataSource.getConfig()
            }
        }

        @Test
        fun `surfaces session expiry when config rejects the host's token too`() = runTest {
            cachedTokens(initial = "rejected-token")
            coEvery { remoteDataSource.getConfig() } returns Result.failure(ChatbotException.SessionExpired())

            val result = repository.initialize()

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
            coVerify(exactly = 1) { chatbotCallbacks.fetchToken() }
            coVerify(exactly = 2) { remoteDataSource.getConfig() }
        }

        @Test
        fun `asks the host again on the next attempt once its token was rejected`() = runTest {
            val tokens = cachedTokens(initial = "rejected-token")
            coEvery { chatbotCallbacks.fetchToken() } returnsMany listOf(
                Result.success("rejected-token"),
                Result.success("valid-token")
            )
            coEvery { remoteDataSource.getConfig() } answers {
                if (tokens.current == "valid-token") Result.success(testConfigRemote)
                else Result.failure(ChatbotException.SessionExpired())
            }

            val first = repository.initialize()
            val retry = repository.initialize()

            assertIs<ChatbotException.SessionExpired>(first.exceptionOrNull())
            assertEquals(testConfigRemote.mapToDomain(), retry.getOrThrow())
            assertEquals("valid-token", tokens.current)
        }
    }

    /** Makes the token cache behave like the in-memory one: set, read back and cleared. */
    private fun cachedTokens(initial: String?): TokenCache {
        val cache = TokenCache(current = initial)
        every { tokenLocalDataSource.getToken() } answers { cache.current }
        every { tokenLocalDataSource.setToken(any()) } answers { cache.current = firstArg() }
        every { tokenLocalDataSource.clear() } answers { cache.current = null }
        return cache
    }

    private class TokenCache(var current: String?)

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

            val result = repository.renewToken(beforeReplacing = {})

            assertTrue(result.isSuccess)
            verify { tokenLocalDataSource.setToken("new-token") }
        }

        @Test
        fun `runs beforeReplacing after the host supplies the token and before caching it`() = runTest {
            val events = mutableListOf<String>()
            coEvery { chatbotCallbacks.createNewToken() } coAnswers {
                events += "host"
                Result.success("new-token")
            }
            every { tokenLocalDataSource.setToken("new-token") } answers { events += "cache" }

            repository.renewToken(beforeReplacing = { events += "beforeReplacing" })

            assertEquals(listOf("host", "beforeReplacing", "cache"), events)
        }

        @Test
        fun `keeps the current token when the host fails`() = runTest {
            coEvery { chatbotCallbacks.createNewToken() } returns Result.failure(IllegalStateException("boom"))
            var ranBeforeReplacing = false

            val result = repository.renewToken(beforeReplacing = { ranBeforeReplacing = true })

            assertTrue(result.isFailure)
            assertFalse(ranBeforeReplacing)
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
                images = emptyList(),
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
        fun `includes image parts on the AI send`() = runTest {
            val messageSlot = slot<SendMessageRemote>()
            every { remoteDataSource.sendMessage(capture(messageSlot)) } returns emptyFlow()
            val image = OutgoingImageInput.create(
                data = "QQ==",
                mime = "image/jpeg",
                filename = null
            )!!

            repository.sendMessage(
                message = "see photo",
                images = listOf(image),
                currentPage = null
            ).test { awaitComplete() }

            assertEquals(
                expected = SendMessageRemote(
                    message = OutgoingMessageRemote(
                        parts = listOf(
                            OutgoingTextPartRemote(text = "see photo"),
                            OutgoingImagePartRemote(
                                data = "QQ==",
                                mime = "image/jpeg",
                                filename = null
                            )
                        )
                    )
                ),
                actual = messageSlot.captured
            )
        }

        @Test
        fun `maps remote events to domain events`() = runTest {
            val events = flowOf(StreamStatusEventRemote(status = "thinking"))
            every { remoteDataSource.sendMessage(any()) } returns events

            repository.sendMessage(
                message = "hello",
                images = emptyList(),
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
                images = emptyList(),
                currentPage = null
            ).test {
                assertIs<StreamStatusEvent>(awaitItem())
                awaitComplete()
            }
        }

        @Test
        fun `makes a bare attachment path absolute against the environment API root`() = runTest {
            val events = flowOf(
                StreamPartEventRemote(part = MessageFileRemote(filename = "a.pdf", url = "/api/v1/files/a"))
            )
            every { remoteDataSource.sendMessage(any()) } returns events

            repository.sendMessage(
                message = "hello",
                images = emptyList(),
                currentPage = null
            ).test {
                val event = assertIs<StreamPartEvent>(awaitItem())
                val file = assertIs<MessageFile>(event.part)
                assertEquals("https://api.dialogintelligens.dk/api/v1/files/a", file.url)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("When getLatestMessage is called")
    inner class GetLatestMessageTest {

        @Test
        fun `reads one message from the newest end of the history and returns it`() = runTest {
            coEvery { remoteDataSource.getMessageHistory(cursor = null, pageSize = 1) } returns Result.success(
                MessagePageRemote(
                    has_more = true,
                    messages = listOf(
                        MessageRemote(
                            message_id = "conv_7_1",
                            role = MessageRoleRemote.AGENT,
                            parts = emptyList(),
                            created_at = "2026-01-01T10:00:00.123456+00:00"
                        )
                    ),
                    next_cursor = "cursor-1"
                )
            )

            val latest = repository.getLatestMessage().getOrNull()

            assertEquals("conv_7_1", latest?.id)
            assertEquals("2026-01-01T10:00:00.123456+00:00", latest?.createdAt)
        }

        @Test
        fun `returns no message for an empty conversation`() = runTest {
            coEvery { remoteDataSource.getMessageHistory(cursor = null, pageSize = 1) } returns Result.success(
                MessagePageRemote(has_more = false, messages = emptyList(), next_cursor = null)
            )

            val result = repository.getLatestMessage()

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @Test
        fun `passes a failed read on unchanged`() = runTest {
            val failure = IllegalStateException("boom")
            coEvery { remoteDataSource.getMessageHistory(cursor = null, pageSize = 1) } returns Result.failure(failure)

            val result = repository.getLatestMessage()

            assertSame(failure, result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("When getMessageHistory is called")
    inner class GetMessageHistoryTest {

        @Test
        fun `reports a loaded refresh before Paging presents its page`() = runTest {
            coEvery { remoteDataSource.getMessageHistory(cursor = null, pageSize = any()) } returns Result.success(
                MessagePageRemote(
                    has_more = false,
                    messages = listOf(
                        MessageRemote(
                            message_id = "message-1",
                            role = MessageRoleRemote.ASSISTANT,
                            parts = listOf(ImagePartRemote(url = "https://example.com/image.png"))
                        )
                    ),
                    next_cursor = null
                )
            )
            val order = mutableListOf<String>()
            val presented = CompletableDeferred<Unit>()
            val presenter = object : PagingDataPresenter<Message>(
                mainContext = UnconfinedTestDispatcher(testScheduler)
            ) {
                override suspend fun presentPagingDataEvent(event: PagingDataEvent<Message>) {
                    if (event is PagingDataEvent.Refresh) {
                        order += "presented"
                        presented.complete(Unit)
                    }
                }
            }

            val collection = launch {
                repository.getMessageHistory(
                    pagingConfig = PagingConfig(pageSize = 10),
                    onRefreshLoaded = { order += "reported" }
                ).collectLatest { presenter.collectFrom(it) }
            }
            presented.await()
            collection.cancel()

            assertEquals(expected = listOf("reported", "presented"), actual = order)
        }
    }

    @Nested
    @DisplayName("When submitAction is called")
    inner class SubmitActionTest {

        private val request = SubmitActionRequest(
            partId = "part_1",
            currentPage = "Checkout",
            action = SubmitAction.ContactForm(fields = mapOf("email" to "a@b.c"))
        )

        @Test
        fun `sends the wire request and returns the mapped response`() = runTest {
            val bodySlot = slot<SubmitActionRequestRemote>()
            coEvery { remoteDataSource.submitAction(capture(bodySlot)) } returns Result.success(
                SubmitActionResponseRemote(submission_id = "sub_1", confirmation_text = "Thanks")
            )

            val result = repository.submitAction(request)

            assertEquals(
                SubmitActionRequestRemote(
                    part_id = "part_1",
                    current_page_url = "Checkout",
                    action = ContactFormActionRemote(fields = mapOf("email" to "a@b.c"))
                ),
                bodySlot.captured
            )
            assertEquals(
                SubmitActionResponse(submissionId = "sub_1", confirmationText = "Thanks"),
                result.getOrNull()
            )
        }

        @Test
        fun `passes the data source's typed failure on unchanged`() = runTest {
            val conflict = FormRequestException.Conflict()
            coEvery { remoteDataSource.submitAction(any()) } returns Result.failure(conflict)

            val result = repository.submitAction(request)

            assertSame(conflict, result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("When fetchForm is called")
    inner class FetchFormTest {

        @Test
        fun `fetches the form by id and returns the mapped definition`() = runTest {
            val definition = ChatFormDefinitionRemote(
                form_id = "salesLead",
                name = " Sales lead ",
                confirmation_text = null,
                fields = listOf(FormFieldRemote(key = "email", label = "Email")),
                min_filled_fields = 1
            )
            coEvery { remoteDataSource.fetchForm("salesLead") } returns Result.success(definition)

            val result = repository.fetchForm("salesLead")

            assertEquals(definition.mapToDomain(), result.getOrNull())
            assertEquals("Sales lead", result.getOrNull()?.name)
        }

        @Test
        fun `passes the data source's typed failure on unchanged`() = runTest {
            val notFound = FormRequestException.NotFound()
            coEvery { remoteDataSource.fetchForm(any()) } returns Result.failure(notFound)

            val result = repository.fetchForm("gone")

            assertSame(notFound, result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("When fetchBanners is called")
    inner class FetchBannersTest {

        @Test
        fun `sends the page trimmed`() = runTest {
            coEvery { remoteDataSource.listBanners(url = any()) } returns
                Result.success(ChatBannerListRemote())

            repository.fetchBanners(pageUrl = "  /products/boots \n")

            coVerify(exactly = 1) { remoteDataSource.listBanners(url = "/products/boots") }
        }

        @Test
        fun `sends a blank page as no page`() = runTest {
            coEvery { remoteDataSource.listBanners(url = any()) } returns
                Result.success(ChatBannerListRemote())

            repository.fetchBanners(pageUrl = "   ")

            coVerify(exactly = 1) { remoteDataSource.listBanners(url = null) }
        }

        @Test
        fun `sends no page when the host gave none`() = runTest {
            coEvery { remoteDataSource.listBanners(url = any()) } returns
                Result.success(ChatBannerListRemote())

            repository.fetchBanners(pageUrl = null)

            coVerify(exactly = 1) { remoteDataSource.listBanners(url = null) }
        }
    }

    @Nested
    @DisplayName("When submitLivechatFeedback is called")
    inner class SubmitLivechatFeedbackTest {

        @Test
        fun `sends the comment trimmed`() = runTest {
            val body = slot<LivechatFeedbackRequestRemote>()
            coEvery { remoteDataSource.submitLivechatFeedback(capture(body)) } returns Result.success(Unit)

            repository.submitLivechatFeedback(rating = 2, feedback = "  Too slow \n")

            assertEquals(LivechatFeedbackRequestRemote(rating = 2, feedback = "Too slow"), body.captured)
        }

        @Test
        fun `sends a blank comment as no comment`() = runTest {
            val body = slot<LivechatFeedbackRequestRemote>()
            coEvery { remoteDataSource.submitLivechatFeedback(capture(body)) } returns Result.success(Unit)

            repository.submitLivechatFeedback(rating = 3, feedback = "   ")

            assertEquals(LivechatFeedbackRequestRemote(rating = 3, feedback = null), body.captured)
        }
    }

    @Nested
    @DisplayName("When rateConversation is called")
    inner class RateConversationTest {

        @Test
        fun `sends the comment trimmed`() = runTest {
            val body = slot<ConversationRatingRequestRemote>()
            coEvery { remoteDataSource.rateConversation(capture(body)) } returns Result.success(Unit)

            repository.rateConversation(rating = 2, feedback = "  Too slow \n")

            assertEquals(ConversationRatingRequestRemote(rating = 2, feedback = "Too slow"), body.captured)
        }

        @Test
        fun `sends a blank comment as no comment`() = runTest {
            val body = slot<ConversationRatingRequestRemote>()
            coEvery { remoteDataSource.rateConversation(capture(body)) } returns Result.success(Unit)

            repository.rateConversation(rating = 3, feedback = "   ")

            assertEquals(ConversationRatingRequestRemote(rating = 3, feedback = null), body.captured)
        }

        @Test
        fun `sends a score without a comment`() = runTest {
            val body = slot<ConversationRatingRequestRemote>()
            coEvery { remoteDataSource.rateConversation(capture(body)) } returns Result.success(Unit)

            repository.rateConversation(rating = 5, feedback = null)

            assertEquals(ConversationRatingRequestRemote(rating = 5, feedback = null), body.captured)
        }
    }
}
