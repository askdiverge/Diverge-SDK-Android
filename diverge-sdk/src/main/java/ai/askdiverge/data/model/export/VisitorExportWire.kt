package ai.askdiverge.data.model.export

/**
 * Path literals for `GET /api/v1/chat/export` (`Visitor.exportMyData` in the Chatbot API TypeSpec).
 * Tests pin these strings.
 */
internal object VisitorExportWire {

    /**
     * Retrofit path relative to the API base (`…/api/`). The authenticated visitor export; the
     * backend decides the JSON shape (`VisitorDataExport`). A new path needs a backend change first.
     */
    const val PATH = "v1/chat/export"
}
