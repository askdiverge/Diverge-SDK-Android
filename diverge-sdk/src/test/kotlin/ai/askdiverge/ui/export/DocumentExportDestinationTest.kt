package ai.askdiverge.ui.export

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class DocumentExportDestinationTest {

    private val uri = mockk<Uri>()
    private val contentResolver = mockk<ContentResolver> {
        every { openOutputStream(uri, "wt") } returns ByteArrayOutputStream()
    }

    @BeforeTest
    fun setUp() {
        mockkStatic(DocumentsContract::class)
        every { DocumentsContract.deleteDocument(contentResolver, uri) } returns true
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(DocumentsContract::class)
    }

    @Test
    fun `the document is opened truncating so none of its earlier content survives`() {
        makeSUT().openOutputStream()

        verify(exactly = 1) { contentResolver.openOutputStream(uri, "wt") }
    }

    @Test
    fun `a document the export opened is deleted`() {
        val sut = makeSUT()
        sut.openOutputStream()

        sut.delete()

        verify(exactly = 1) { DocumentsContract.deleteDocument(contentResolver, uri) }
    }

    @Test
    fun `the empty document the save dialog created is deleted though it was never opened`() {
        givenDocumentSize(0L)

        makeSUT().delete()

        verify(exactly = 1) { DocumentsContract.deleteDocument(contentResolver, uri) }
    }

    @Test
    fun `an existing document the export never opened is kept`() {
        givenDocumentSize(2_048L)

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `a document the provider answers no size query for is kept`() {
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns null

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `a document the provider returns no row for is kept`() {
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { getColumnIndex(DocumentsContract.Document.COLUMN_SIZE) } returns 0
            every { moveToFirst() } returns false
        }
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `a document whose size the provider leaves empty is kept`() {
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { getColumnIndex(DocumentsContract.Document.COLUMN_SIZE) } returns 0
            every { moveToFirst() } returns true
            every { isNull(0) } returns true
            every { getLong(0) } returns 0L
        }
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `a document the provider returns no size column for is kept`() {
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { getColumnIndex(DocumentsContract.Document.COLUMN_SIZE) } returns -1
            every { moveToFirst() } returns true
            every { isNull(0) } returns false
            every { getLong(0) } returns 0L
        }
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `an existing document is kept when the provider puts another column before its size`() {
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { getColumnIndex(DocumentsContract.Document.COLUMN_SIZE) } returns 1
            every { moveToFirst() } returns true
            every { isNull(0) } returns false
            every { getLong(0) } returns 0L
            every { isNull(1) } returns false
            every { getLong(1) } returns 2_048L
        }
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor

        makeSUT().delete()

        verify(exactly = 0) { DocumentsContract.deleteDocument(any(), any()) }
    }

    @Test
    fun `a delete the provider refuses is reported`() {
        every { DocumentsContract.deleteDocument(contentResolver, uri) } returns false
        val sut = makeSUT()
        sut.openOutputStream()

        assertFailsWith<IOException> { sut.delete() }
    }

    private fun givenDocumentSize(size: Long) {
        val cursor = mockk<Cursor>(relaxUnitFun = true) {
            every { getColumnIndex(DocumentsContract.Document.COLUMN_SIZE) } returns 0
            every { moveToFirst() } returns true
            every { isNull(0) } returns false
            every { getLong(0) } returns size
        }
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns cursor
    }

    private fun makeSUT() = DocumentExportDestination(contentResolver = contentResolver, uri = uri)
}
