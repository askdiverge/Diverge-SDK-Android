package ai.askdiverge.ui.livechat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class LivechatRatingModelTest {

    @Test
    fun `scores up to 3 open the comment step and 4 or 5 send at once`() {
        val model = LivechatRatingModel()

        val three = assertNotNull(model.pick(3))

        assertEquals(LivechatRatingModel.Phase.Feedback(score = 3), three.phase)
        assertTrue(three.needsComment)
        assertNull(model.pick(4))
        assertNull(model.pick(5))
    }

    @Test
    fun `a failed send keeps the score and comment and editing returns to the comment step`() {
        val draft = assertNotNull(LivechatRatingModel().pick(2)).withComment("Too slow")
        val submitting = assertNotNull(draft.beginSubmitting(score = 2, feedback = "Too slow"))

        val failed = submitting.markFailed()

        assertEquals(LivechatRatingModel.Phase.Failed(score = 2, feedback = "Too slow"), failed.phase)
        assertEquals("Too slow", failed.comment)
        assertTrue(failed.needsComment)
        val edited = failed.withComment("Too slow!")
        assertEquals(LivechatRatingModel.Phase.Feedback(score = 2), edited.phase)
        assertEquals("Too slow!", edited.comment)
    }

    @Test
    fun `nothing changes while a send is in flight`() {
        val submitting = assertNotNull(LivechatRatingModel().beginSubmitting(score = 5, feedback = null))

        assertNull(submitting.beginSubmitting(score = 5, feedback = null))
        assertSame(submitting, submitting.pick(1))
    }
}
