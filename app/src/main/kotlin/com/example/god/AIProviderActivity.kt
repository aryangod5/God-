package com.example.god

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AIProviderActivity : AppCompatActivity() {

    private lateinit var providerName: EditText
    private lateinit var endpoint: EditText
    private lateinit var apiKey: EditText
    private lateinit var modelName: EditText
    private lateinit var enabledSwitch: Switch

    private lateinit var storage: AIProviderStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storage = AIProviderStorage(this)

        createInterface()
        loadSavedConfiguration()
    }

    private fun createInterface() {

        val scrollView =
            android.widget.ScrollView(this)

        val layout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(35, 50, 35, 50)
            }

        val title =
            TextView(this).apply {
                text = "GOD AI Provider"
                textSize = 30f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 30)
            }

        layout.addView(title)

        providerName =
            createField(
                "AI Provider Name",
                "Example: My AI"
            )

        layout.addView(providerName)

        endpoint =
            createField(
                "API Endpoint",
                "Enter the API endpoint"
            )

        endpoint.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_URI

        layout.addView(endpoint)

        apiKey =
            createField(
                "API Key",
                "Enter your API key"
            )

        apiKey.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

        layout.addView(apiKey)

        modelName =
            createField(
                "Model Name",
                "Example: your-model"
            )

        layout.addView(modelName)

        enabledSwitch =
            Switch(this).apply {
                text = "Enable this AI provider"
                textSize = 17f
                setPadding(0, 25, 0, 25)
            }

        layout.addView(enabledSwitch)

        val saveButton =
            Button(this).apply {
                text = "Save AI Configuration"

                setOnClickListener {
                    saveConfiguration()
                }
            }

        layout.addView(saveButton)

        val testButton =
            Button(this).apply {
                text = "Test Connection"

                setOnClickListener {
                    testConfiguration()
                }
            }

        layout.addView(testButton)

        val clearButton =
            Button(this).apply {
                text = "Clear Configuration"

                setOnClickListener {
                    clearConfiguration()
                }
            }

        layout.addView(clearButton)

        val securityNote =
            TextView(this).apply {
                text =
                    "\nSecurity:\n" +
                            "Your API key should never be placed directly " +
                            "inside the source code or GitHub repository.\n\n" +
                            "The final GOD version will use Android secure " +
                            "storage for sensitive credentials."

                textSize = 15f
                setPadding(0, 25, 0, 0)
            }

        layout.addView(securityNote)

        scrollView.addView(layout)

        setContentView(scrollView)
    }

    private fun createField(
        label: String,
        hintText: String
    ): EditText {

        return EditText(this).apply {

            hint = hintText

            textSize = 16f

            setHintTextColor(Color.GRAY)

            contentDescription = label

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
        }
    }

    private fun saveConfiguration() {

        val config =
            AIProviderConfig(
                providerName =
                    providerName.text.toString().trim(),

                endpoint =
                    endpoint.text.toString().trim(),

                apiKey =
                    apiKey.text.toString(),

                modelName =
                    modelName.text.toString().trim(),

                enabled =
                    enabledSwitch.isChecked
            )

        if (config.providerName.isBlank() ||
            config.endpoint.isBlank() ||
            config.apiKey.isBlank() ||
            config.modelName.isBlank()
        ) {

            Toast.makeText(
                this,
                "Please fill in all AI provider fields.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        storage.save(config)

        Toast.makeText(
            this,
            "AI provider configuration saved.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadSavedConfiguration() {

        val config =
            storage.load()

        providerName.setText(config.providerName)
        endpoint.setText(config.endpoint)
        apiKey.setText(config.apiKey)
        modelName.setText(config.modelName)
        enabledSwitch.isChecked = config.enabled
    }

    private fun testConfiguration() {

        val config =
            AIProviderConfig(
                providerName =
                    providerName.text.toString().trim(),

                endpoint =
                    endpoint.text.toString().trim(),

                apiKey =
                    apiKey.text.toString(),

                modelName =
                    modelName.text.toString().trim(),

                enabled =
                    enabledSwitch.isChecked
            )

        if (config.endpoint.isBlank()) {

            Toast.makeText(
                this,
                "Enter an API endpoint first.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * The real network/API adapter will be added
         * in the next AI integration step.
         */
        Toast.makeText(
            this,
            "Configuration is ready. " +
                    "The real API connection will be added next.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun clearConfiguration() {

        storage.clear()

        providerName.text.clear()
        endpoint.text.clear()
        apiKey.text.clear()
        modelName.text.clear()
        enabledSwitch.isChecked = false

        Toast.makeText(
            this,
            "AI provider configuration cleared.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
