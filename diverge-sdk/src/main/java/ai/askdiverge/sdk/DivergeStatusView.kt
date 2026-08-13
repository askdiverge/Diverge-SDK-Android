package ai.askdiverge.sdk

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight status view showing SDK version and configured environment.
 *
 * Built programmatically (no layout XML) so host apps do not pull in View XML from the SDK.
 */
class DivergeStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val versionView: TextView
    private val environmentView: TextView
    private val urlView: TextView

    init {
        orientation = VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        titleView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            text = context.getString(R.string.diverge_status_title)
            setTextColor(COLOR_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = context.getString(R.string.diverge_status_title)
        }
        versionView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
            setTextColor(COLOR_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        environmentView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * resources.displayMetrics.density).toInt()
            }
            setTextColor(COLOR_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        urlView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * resources.displayMetrics.density).toInt()
            }
            setTextColor(COLOR_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        addView(titleView)
        addView(versionView)
        addView(environmentView)
        addView(urlView)
        bind(null)
    }

    fun bind(client: DivergeClient?) {
        versionView.text = context.getString(R.string.diverge_status_version, Diverge.VERSION)
        versionView.contentDescription = versionView.text

        if (client != null) {
            val env = client.configuration.environment.wireName
            environmentView.text = context.getString(R.string.diverge_status_environment, env)
            environmentView.contentDescription = environmentView.text
            urlView.text = client.apiBaseUrl
            urlView.contentDescription = "API base URL ${client.apiBaseUrl}"
            urlView.visibility = View.VISIBLE
        } else {
            environmentView.text = context.getString(R.string.diverge_status_not_configured)
            environmentView.contentDescription = environmentView.text
            urlView.text = ""
            urlView.visibility = View.GONE
        }
    }

    companion object {
        private const val COLOR_PRIMARY = 0xFF1A1A1A.toInt()
        private const val COLOR_SECONDARY = 0xFF5C5C5C.toInt()

        /** Stable dump for tests (mirrors iOS ``DivergeStatusView.accessibilityDump``). */
        @JvmStatic
        fun accessibilityDump(client: DivergeClient?): String {
            val lines = mutableListOf(
                "title: Diverge SDK",
                "version: ${Diverge.VERSION}",
            )
            if (client != null) {
                lines += "environment: ${client.configuration.environment.wireName}"
                lines += "apiBaseURL: ${client.apiBaseUrl}"
            } else {
                lines += "state: not-configured"
            }
            return lines.joinToString("\n")
        }
    }
}
