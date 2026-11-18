package ai.askdiverge.ui.form

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.state.SnackbarType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
internal class FormCoordinatorTest {

    private val repository: ChatbotRepository = mockk()
    private var sessionExpiredCount = 0

    private val photoForm = ConversationForm.custom(
        ShowForm(
            partId = "p1",
            formId = "claim",
            name = "Claim",
            confirmationText = null,
            fields = listOf(field(key = "receipt", type = FormField.FieldType.FILE, required = true)),
            minFilledFields = 0
        )
    )

    private val contactForm = ConversationForm.contact(
        ShowContactForm(
            partId = "contact",
            fields = listOf(
                field(key = "name", required = true),
                field(key = "email", type = FormField.FieldType.EMAIL, required = true)
            )
        )
    )

    private val thinForm = ConversationForm.custom(
        ShowForm(
            partId = "thin",
            formId = "claim",
            name = null,
            confirmationText = null,
            fields = emptyList(),
            minFilledFields = 0
        )
    )

    @Nested
    @DisplayName("Photos")
    inner class Photos {

        @Test
        fun `a photo that cannot be read shows the photo banner and releases the encoding flag`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.ensureForm(photoForm)

            sut.onPhotoPicked(FormPhotoDestination.FormField(partId = "p1", key = "receipt")) { null }

            val state = holder.uiState.value
            assertFalse(state.isEncodingAttachment)
            assertEquals(FormBannerError.PhotoFailed, state.formModels.getValue("p1").formError)
            assertFalse(state.formModels.getValue("p1").files.containsKey("receipt"))
        }

        @Test
        fun `a photo whose stream throws still releases the encoding flag`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.ensureForm(photoForm)

            sut.onPhotoPicked(FormPhotoDestination.FormField(partId = "p1", key = "receipt")) {
                throw SecurityException("revoked")
            }

            assertFalse(holder.uiState.value.isEncodingAttachment)
            assertEquals(FormBannerError.PhotoFailed, holder.uiState.value.formModels.getValue("p1").formError)
        }

        @Test
        fun `a photo for a form the session no longer has reports that it was not attached`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)

            sut.onPhotoPicked(FormPhotoDestination.TicketAttachment(partId = "gone")) { null }

            assertEquals(SnackbarType.PhotoAttachFailed, holder.uiState.value.snackbarState?.type)
            assertFalse(holder.uiState.value.isEncodingAttachment)
        }
    }

    @Nested
    @DisplayName("Submitting")
    inner class Submitting {

        @Test
        fun `a valid form is sent once with the current page and shows the server's confirmation`() = runTest {
            coEvery { repository.submitAction(any()) } returns
                Result.success(SubmitActionResponse(submissionId = "s1", confirmationText = "Thanks, Ada"))
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder, currentPage = "checkout")
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")

            sut.onSubmit("contact")

            assertEquals(
                FormSubmissionModel.Phase.Submitted("Thanks, Ada"),
                holder.uiState.value.formModels.getValue("contact").phase
            )
            coVerify(exactly = 1) {
                repository.submitAction(
                    SubmitActionRequest(
                        partId = "contact",
                        currentPage = "checkout",
                        action = SubmitAction.ContactForm(
                            fields = mapOf("name" to "Ada", "email" to "ada@example.com")
                        )
                    )
                )
            }
        }

        @Test
        fun `a blank required field shows its error and nothing is sent`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada")

            sut.onSubmit("contact")

            val model = holder.uiState.value.formModels.getValue("contact")
            assertEquals(FormFieldError.REQUIRED, model.fieldErrors["email"])
            assertEquals(FormSubmissionModel.Phase.Editing, model.phase)
            coVerify(exactly = 0) { repository.submitAction(any()) }
        }

        @Test
        fun `a support ticket asks for name, email and message even when the marker leaves them optional`() = runTest {
            coEvery { repository.submitAction(any()) } returns
                Result.success(SubmitActionResponse(submissionId = "s1", confirmationText = null))
            val ticket = assertNotNull(
                ConversationForm.supportTicket(
                    ShowSupportTicket(
                        partId = "ticket",
                        fields = listOf(
                            field(key = "name"),
                            field(key = "email", type = FormField.FieldType.EMAIL),
                            field(key = "message", type = FormField.FieldType.TEXTAREA)
                        ),
                        attachmentsAccepted = false,
                        maxAttachmentSizeBytes = 0
                    )
                )
            )
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(ticket, "name" to "Ada")

            sut.onSubmit("ticket")

            val model = holder.uiState.value.formModels.getValue("ticket")
            assertEquals(
                mapOf("email" to FormFieldError.REQUIRED, "message" to FormFieldError.REQUIRED),
                model.fieldErrors
            )
            assertEquals(FormSubmissionModel.Phase.Editing, model.phase)
            coVerify(exactly = 0) { repository.submitAction(any()) }

            sut.onValueChanged(partId = "ticket", key = "email", value = "ada@example.com")
            sut.onValueChanged(partId = "ticket", key = "message", value = "My order is late")
            sut.onSubmit("ticket")

            assertIs<FormSubmissionModel.Phase.Submitted>(holder.uiState.value.formModels.getValue("ticket").phase)
        }

        @Test
        fun `a form is not sent while a reply is still streaming`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")
            holder.onReplyStreaming(emptyList())

            sut.onSubmit("contact")

            assertEquals(FormSubmissionModel.Phase.Editing, holder.uiState.value.formModels.getValue("contact").phase)
            coVerify(exactly = 0) { repository.submitAction(any()) }
        }

        @Test
        fun `a field the server rejects is marked on the form, which stays editable`() = runTest {
            coEvery { repository.submitAction(any()) } returns Result.failure(
                FormRequestException.Validation(
                    params = listOf(FormRequestException.Validation.Param(field = "action.fields.email", message = "bad"))
                )
            )
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")

            sut.onSubmit("contact")

            val model = holder.uiState.value.formModels.getValue("contact")
            assertEquals(mapOf("email" to FormFieldError.REJECTED), model.fieldErrors)
            assertEquals(FormSubmissionModel.Phase.Editing, model.phase)
        }

        @Test
        fun `a form the server already has shows as already sent`() = runTest {
            coEvery { repository.submitAction(any()) } returns Result.failure(FormRequestException.Conflict())
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")

            sut.onSubmit("contact")

            assertEquals(
                FormSubmissionModel.Phase.Submitted(FormSubmissionModel.ALREADY_SENT_TOKEN),
                holder.uiState.value.formModels.getValue("contact").phase
            )
        }

        @Test
        fun `a send that fails can be retried`() = runTest {
            coEvery { repository.submitAction(any()) } returnsMany listOf(
                Result.failure(IOException("offline")),
                Result.success(SubmitActionResponse(submissionId = "s1", confirmationText = null))
            )
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")

            sut.onSubmit("contact")

            assertEquals(
                FormSubmissionModel.Phase.Failed(FormBannerError.SubmitFailed),
                holder.uiState.value.formModels.getValue("contact").phase
            )

            sut.onSubmit("contact")

            assertEquals(
                FormSubmissionModel.Phase.Submitted(""),
                holder.uiState.value.formModels.getValue("contact").phase
            )
        }

        @Test
        fun `an expired session while sending is reported to the session`() = runTest {
            coEvery { repository.submitAction(any()) } returns Result.failure(ChatbotException.SessionExpired())
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")

            sut.onSubmit("contact")

            assertEquals(1, sessionExpiredCount)
        }

        @Test
        fun `a send that ends after the session was replaced changes nothing`() = runTest {
            val response = CompletableDeferred<Result<SubmitActionResponse>>()
            coEvery { repository.submitAction(any()) } coAnswers { response.await() }
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.fill(contactForm, "name" to "Ada", "email" to "ada@example.com")
            sut.onSubmit("contact")

            sut.cancelPending()
            response.complete(Result.failure(ChatbotException.SessionExpired()))

            assertEquals(0, sessionExpiredCount)
        }
    }

    @Nested
    @DisplayName("Fetching a thin custom form")
    inner class Hydrating {

        private val definition = ChatFormDefinition(
            formId = "claim",
            name = "Warranty claim",
            confirmationText = "We will be in touch",
            fields = listOf(field(key = "order", required = true)),
            minFilledFields = 0
        )

        @Test
        fun `a form without fields fetches its definition and shows its fields`() = runTest {
            coEvery { repository.fetchForm("claim") } returns Result.success(definition)
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)

            sut.ensureForm(thinForm)

            val model = holder.uiState.value.formModels.getValue("thin")
            assertEquals(definition.fields, model.form.fields)
            assertEquals("Warranty claim", model.form.displayName)
            assertFalse(model.isHydrating)
        }

        @Test
        fun `a form that arrives with its fields is not fetched`() = runTest {
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)

            sut.ensureForm(photoForm)

            coVerify(exactly = 0) { repository.fetchForm(any()) }
        }

        @Test
        fun `a form the backend no longer has shows as unavailable`() = runTest {
            coEvery { repository.fetchForm("claim") } returns Result.failure(FormRequestException.NotFound())
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)

            sut.ensureForm(thinForm)

            val model = holder.uiState.value.formModels.getValue("thin")
            assertTrue(model.isUnavailable)
            assertFalse(model.hydrationFailed)
        }

        @Test
        fun `a fetch that fails can be retried`() = runTest {
            coEvery { repository.fetchForm("claim") } returnsMany listOf(
                Result.failure(IOException("offline")),
                Result.success(definition)
            )
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.ensureForm(thinForm)

            assertTrue(holder.uiState.value.formModels.getValue("thin").hydrationFailed)

            sut.onReload("thin")

            val model = holder.uiState.value.formModels.getValue("thin")
            assertFalse(model.hydrationFailed)
            assertEquals(definition.fields, model.form.fields)
        }

        @Test
        fun `an expired session while fetching is reported to the session`() = runTest {
            coEvery { repository.fetchForm("claim") } returns Result.failure(ChatbotException.NoTokenAvailable())
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)

            sut.ensureForm(thinForm)

            assertEquals(1, sessionExpiredCount)
        }

        @Test
        fun `a fetch that ends after the session was replaced changes nothing`() = runTest {
            val response = CompletableDeferred<Result<ChatFormDefinition>>()
            coEvery { repository.fetchForm("claim") } coAnswers { response.await() }
            val holder = ChatbotUiStateHolder()
            val sut = makeSUT(holder)
            sut.ensureForm(thinForm)

            sut.cancelPending()
            response.complete(Result.failure(ChatbotException.SessionExpired()))

            assertEquals(0, sessionExpiredCount)
        }
    }

    private fun FormCoordinator.fill(
        form: ConversationForm,
        vararg answers: Pair<String, String>
    ) {
        ensureForm(form)
        answers.forEach { (key, value) -> onValueChanged(partId = form.partId, key = key, value = value) }
    }

    private fun TestScope.makeSUT(
        holder: ChatbotUiStateHolder,
        currentPage: String? = null
    ): FormCoordinator {
        val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        return FormCoordinator(
            uiStateHolder = holder,
            submitActionUseCase = SubmitActionUseCase(repository),
            fetchFormUseCase = FetchFormUseCase(repository),
            scope = CoroutineScope(dispatcher),
            currentPage = currentPage,
            offersAttachments = true,
            encodeDispatcher = dispatcher,
            onSessionExpired = { sessionExpiredCount += 1 },
            defaultConfirmation = { "" }
        )
    }

    private fun field(
        key: String,
        type: FormField.FieldType = FormField.FieldType.TEXT,
        required: Boolean = false
    ) = FormField(
        key = key,
        label = key,
        placeholder = null,
        type = type,
        required = required,
        options = emptyList(),
        visibleWhen = emptyList()
    )
}
