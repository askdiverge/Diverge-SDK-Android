package ai.askdiverge.sdk

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight status view showing SDK version and configured environment.
 *
 * Built programmatically (no layout XML) so host apps do not pull in View XML from the SDK.
 *
 * Text colors are AA-safe on white / light surfaces (primary ≈17:1, secondary ≈8.9:1).
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
        // Keep children individually focusable for TalkBack (do not set a parent contentDescription).
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        titleView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            text = context.getString(R.string.diverge_status_title)
            setTextColor(COLOR_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = context.getString(R.string.diverge_status_title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isAccessibilityHeading = true
            }
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
            accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
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
            urlView.contentDescription =
                context.getString(R.string.diverge_status_api_base_url, client.apiBaseUrl)
            urlView.visibility = View.VISIBLE
        } else {
            environmentView.text = context.getString(R.string.diverge_status_not_configured)
            environmentView.contentDescription = environmentView.text
            urlView.text = ""
            urlView.visibility = View.GONE
        }
    }

    companion object {
        /** Near-black body text — ≈17:1 on white. */
        private const val COLOR_PRIMARY = 0xFF1A1A1A.toInt()
        /** Secondary / footnote — ≈8.9:1 on white (≥ 4.5:1 AA). */
        private const val COLOR_SECONDARY = 0xFF4A4A4A.toInt()

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
