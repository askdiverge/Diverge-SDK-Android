package ai.askdiverge.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import ai.askdiverge.sdk.Configuration
import ai.askdiverge.sdk.Diverge
import ai.askdiverge.sdk.DivergeException
import ai.askdiverge.sdk.DivergeStatusView
import ai.askdiverge.sdk.Environment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusView = findViewById<DivergeStatusView>(R.id.statusView)
        val apiKeyInput = findViewById<TextInputEditText>(R.id.apiKeyInput)
        val configureButton = findViewById<Button>(R.id.configureButton)
        val errorText = findViewById<TextView>(R.id.errorText)

        findViewById<TextView>(R.id.titleText).apply {
            contentDescription = getString(R.string.app_name)
        }
        findViewById<TextView>(R.id.instructionsText).contentDescription =
            getString(R.string.configure_instructions)
        apiKeyInput.contentDescription = getString(R.string.api_key_hint)
        configureButton.contentDescription = getString(R.string.configure_sandbox)
        statusView.bind(if (Diverge.isConfigured) Diverge.shared else null)

        configureButton.setOnClickListener {
            val key = apiKeyInput.text?.toString().orEmpty()
            try {
                val client = Diverge.configure(
                    Configuration(apiKey = key, environment = Environment.SANDBOX),
                )
                statusView.bind(client)
                errorText.visibility = View.GONE
                errorText.text = ""
                Toast.makeText(this, R.string.configured_sandbox, Toast.LENGTH_SHORT).show()
            } catch (error: DivergeException) {
                statusView.bind(null)
                val message = error.message ?: getString(R.string.configure_failed)
                errorText.text = message
                errorText.contentDescription = getString(R.string.error_prefix, message)
                errorText.visibility = View.VISIBLE
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
