package ai.askdiverge.ui.rating

import ai.askdiverge.ui.livechat.LivechatRatingModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RatingPromptTest {

    private var draft: LivechatRatingModel? = LivechatRatingModel()
    private val submitted = mutableListOf<Pair<Int, String?>>()

    @Test
    fun `a score above the threshold is submitted at once without a comment`() {
        val sut = makeSUT()

        sut.selectScore(4)

        assertEquals(listOf<Pair<Int, String?>>(4 to null), submitted)
    }

    @Test
    fun `a score at or below the threshold opens the comment step and submits nothing`() {
        val sut = makeSUT()

        sut.selectScore(3)

        assertEquals(LivechatRatingModel.Phase.Feedback(score = 3), draft?.phase)
        assertTrue(submitted.isEmpty())
    }

    @Test
    fun `Send submits the picked score with the comment typed`() {
        val sut = makeSUT()
        sut.selectScore(1)
        sut.setComment("Too slow")

        sut.send()

        assertEquals(listOf<Pair<Int, String?>>(1 to "Too slow"), submitted)
    }

    @Test
    fun `Send submits nothing before a score is picked`() {
        val sut = makeSUT()

        sut.send()

        assertTrue(submitted.isEmpty())
    }

    @Test
    fun `a draft already on its way does not start a second submission`() {
        val sut = makeSUT()

        val first = assertNotNull(sut.beginSubmitting(score = 5, feedback = null))

        assertEquals(LivechatRatingModel.Phase.Submitting(score = 5, feedback = null), first.phase)
        assertEquals(first, draft)
        assertNull(sut.beginSubmitting(score = 5, feedback = null))
    }

    @Test
    fun `nothing happens without a draft`() {
        draft = null
        val sut = makeSUT()

        sut.selectScore(5)
        sut.setComment("Too slow")
        sut.send()

        assertNull(draft)
        assertNull(sut.beginSubmitting(score = 5, feedback = null))
        assertTrue(submitted.isEmpty())
    }

    @Test
    fun `drop removes the draft`() {
        val sut = makeSUT()
        sut.selectScore(2)

        sut.drop()

        assertNull(draft)
    }

    private fun makeSUT() = RatingPrompt(
        read = { draft },
        write = { draft = it },
        submit = { score, feedback -> submitted += score to feedback }
    )
}
