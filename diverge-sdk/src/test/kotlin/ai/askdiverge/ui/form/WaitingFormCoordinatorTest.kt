package ai.askdiverge.ui.form

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatSessionState
import ai.askdiverge.domain.model.form.ChatSessionValue
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.GetChatSessionUseCase
import ai.askdiverge.domain.usecase.PatchFormValuesUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class WaitingFormCoordinatorTest {

    @Test
    fun `waiting status loads the card from form and session`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.fetchForm("wait-1") } returns Result.success(sampleDefinition())
        coEvery { repository.getSession() } returns Result.success(
            ChatSessionState(values = listOf(ChatSessionValue(key = "email", value = "a@b.c")))
        )
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)

        sut.configure("wait-1")
        sut.statusChanged(LivechatState.Status.WAITING)
        runCurrent()

        val model = assertNotNull(holder.uiState.value.livechat.waitingForm)
        assertEquals("a@b.c", model.values["email"])
    }

    @Test
    fun `leaving waiting clears the card`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.fetchForm(any()) } returns Result.success(sampleDefinition())
        coEvery { repository.getSession() } returns Result.success(ChatSessionState(emptyList()))
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.configure("wait-1")
        sut.statusChanged(LivechatState.Status.WAITING)
        runCurrent()
        assertNotNull(holder.uiState.value.livechat.waitingForm)

        sut.statusChanged(LivechatState.Status.ACTIVE)

        assertNull(holder.uiState.value.livechat.waitingForm)
    }

    @Test
    fun `a save patches only changed values`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.fetchForm(any()) } returns Result.success(sampleDefinition())
        coEvery { repository.getSession() } returns Result.success(ChatSessionState(emptyList()))
        coEvery { repository.patchFormValues(any(), any()) } returns Result.success(
            ChatSessionState(values = listOf(ChatSessionValue(key = "email", value = "a@b.c")))
        )
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.configure("wait-1")
        sut.statusChanged(LivechatState.Status.WAITING)
        runCurrent()
        sut.onValueChanged(key = "email", value = "a@b.c")

        sut.save()
        runCurrent()

        coVerify { repository.patchFormValues("wait-1", mapOf("email" to "a@b.c")) }
        assertEquals(WaitingFormModel.Phase.Saved, holder.uiState.value.livechat.waitingForm?.phase)
    }

    @Test
    fun `a 409 on save drops the card`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.fetchForm(any()) } returns Result.success(sampleDefinition())
        coEvery { repository.getSession() } returns Result.success(ChatSessionState(emptyList()))
        coEvery { repository.patchFormValues(any(), any()) } returns Result.failure(
            FormRequestException.Conflict()
        )
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.configure("wait-1")
        sut.statusChanged(LivechatState.Status.WAITING)
        runCurrent()
        sut.onValueChanged(key = "email", value = "a@b.c")

        sut.save()
        runCurrent()

        assertNull(holder.uiState.value.livechat.waitingForm)
    }

    @Test
    fun `hidden chat does not keep retrying a failed load`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.fetchForm(any()) } returns Result.failure(IllegalStateException("net"))
        coEvery { repository.getSession() } returns Result.failure(IllegalStateException("net"))
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.configure("wait-1")
        sut.statusChanged(LivechatState.Status.WAITING)
        runCurrent()
        coVerify(exactly = 1) { repository.fetchForm(any()) }

        sut.setVisible(false)
        advanceTimeBy(WaitingFormCoordinator.FIRST_RETRY_MS + 1)
        runCurrent()

        coVerify(exactly = 1) { repository.fetchForm(any()) }
    }

    private fun TestScope.makeSUT(
        holder: ChatbotUiStateHolder,
        repository: ChatbotRepository
    ) = WaitingFormCoordinator(
        uiStateHolder = holder,
        fetchFormUseCase = FetchFormUseCase(repository),
        getChatSessionUseCase = GetChatSessionUseCase(repository),
        patchFormValuesUseCase = PatchFormValuesUseCase(repository),
        scope = this,
        delayMs = { kotlinx.coroutines.delay(it) },
        onSessionExpired = {}
    )

    private fun sampleDefinition() = ChatFormDefinition(
        formId = "wait-1",
        name = "Waiting",
        confirmationText = null,
        fields = listOf(
            FormField(
                key = "email",
                label = "Email",
                placeholder = null,
                type = FormField.FieldType.EMAIL,
                required = false,
                options = emptyList(),
                visibleWhen = emptyList()
            )
        ),
        minFilledFields = 0
    )
}
