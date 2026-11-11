package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.state.SnackbarType
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class FormCoordinatorTest {

    private val form = ConversationForm.custom(
        ShowForm(
            partId = "p1",
            formId = "claim",
            name = "Claim",
            confirmationText = null,
            fields = listOf(
                FormField(
                    key = "receipt",
                    label = "Receipt",
                    placeholder = null,
                    type = FormField.FieldType.FILE,
                    required = true,
                    options = emptyList(),
                    visibleWhen = emptyList()
                )
            ),
            minFilledFields = 0
        )
    )

    @Test
    fun `a photo that cannot be read shows the photo banner and releases the encoding flag`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, UnconfinedTestDispatcher(testScheduler))
        sut.ensureForm(form)

        sut.onPhotoPicked(FormPhotoDestination.FormField(partId = "p1", key = "receipt")) { null }

        val state = holder.uiState.value
        assertFalse(state.isEncodingAttachment)
        assertEquals(FormBannerError.PhotoFailed, state.formModels.getValue("p1").formError)
        assertFalse(state.formModels.getValue("p1").files.containsKey("receipt"))
    }

    @Test
    fun `a photo whose stream throws still releases the encoding flag`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, UnconfinedTestDispatcher(testScheduler))
        sut.ensureForm(form)

        sut.onPhotoPicked(FormPhotoDestination.FormField(partId = "p1", key = "receipt")) {
            throw SecurityException("revoked")
        }

        assertFalse(holder.uiState.value.isEncodingAttachment)
        assertEquals(FormBannerError.PhotoFailed, holder.uiState.value.formModels.getValue("p1").formError)
    }

    @Test
    fun `a photo for a form the session no longer has reports that it was not attached`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, UnconfinedTestDispatcher(testScheduler))

        sut.onPhotoPicked(FormPhotoDestination.TicketAttachment(partId = "gone")) { null }

        assertEquals(SnackbarType.PhotoAttachFailed, holder.uiState.value.snackbarState?.type)
        assertFalse(holder.uiState.value.isEncodingAttachment)
    }

    private fun makeSUT(
        holder: ChatbotUiStateHolder,
        dispatcher: CoroutineDispatcher
    ): FormCoordinator {
        val repository: ChatbotRepository = mockk()
        return FormCoordinator(
            uiStateHolder = holder,
            submitActionUseCase = SubmitActionUseCase(repository),
            fetchFormUseCase = FetchFormUseCase(repository),
            scope = CoroutineScope(dispatcher),
            currentPage = null,
            offersAttachments = true,
            encodeDispatcher = dispatcher,
            onSessionExpired = {},
            defaultConfirmation = { "" }
        )
    }
}
