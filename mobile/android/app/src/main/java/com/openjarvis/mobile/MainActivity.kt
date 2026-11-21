package com.openjarvis.mobile

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val executor = Executors.newSingleThreadExecutor()
    private val history = JSONArray()
    private lateinit var prompt: EditText
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var status: TextView
    private lateinit var sendButton: TextView
    private lateinit var micButton: TextView
    private var tts: TextToSpeech? = null
    private var sessionKey: String = ""
    private var busy = false

    companion object {
        private const val SPEECH_REQUEST = 4101
        private const val AUDIO_PERMISSION_REQUEST = 4102
        private const val PREFS = "openjarvis_mobile"
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val MAX_CONTEXT_MESSAGES = 20
        private const val BG = 0xFF090F18.toInt()
        private const val PANEL = 0xFF111C2A.toInt()
        private const val USER_BUBBLE = 0xFF123B55.toInt()
        private const val ACCENT = 0xFF57D6FF.toInt()
        private const val TEXT = 0xFFEAF3FC.toInt()
        private const val MUTED = 0xFF8FA3B8.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun rounded(color: Int, radius: Float = 18f, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius * resources.displayMetrics.density
            if (stroke != null) setStroke((1 * resources.displayMetrics.density).toInt(), stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(14))
        }
        val brandColumn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandColumn.addView(text("J.A.R.V.I.S", 21f, ACCENT, true))
        brandColumn.addView(text("ASSISTENTE PESSOAL", 9f, MUTED, true).apply { letterSpacing = 0.16f })
        header.addView(brandColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val settingsButton = text("⚙", 25f, TEXT).apply {
            gravity = Gravity.CENTER
            background = rounded(PANEL, 16f)
            setOnClickListener { showConnectionSettings() }
        }
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)

        val connectionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(PANEL, 14f)
        }
        connectionBar.addView(text("●", 10f, 0xFFFFC66D.toInt()))
        status = text("  Configure a conexão para começar", 12f, MUTED)
        connectionBar.addView(status)
        root.addView(connectionBar)

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(14), 0, dp(12))
        }
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(8), dp(7), dp(7), dp(7))
            background = rounded(PANEL, 22f, 0xFF26384A.toInt())
        }
        prompt = EditText(this).apply {
            hint = "Mensagem para o Jarvis…"
            textSize = 15f
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            setPadding(dp(8), dp(9), dp(6), dp(9))
            background = null
            minLines = 1
            maxLines = 4
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        composer.addView(prompt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        micButton = text("🎙", 19f, TEXT).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF1B2B3D.toInt(), 16f)
            setOnClickListener { startSpeechInput() }
        }
        composer.addView(micButton, LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginStart = dp(4) })
        sendButton = text("↑", 25f, BG, true).apply {
            gravity = Gravity.CENTER
            background = rounded(ACCENT, 16f)
            setOnClickListener { sendMessage() }
        }
        composer.addView(sendButton, LinearLayout.LayoutParams(dp(46), dp(46)).apply { marginStart = dp(6) })
        root.addView(composer)
        root.addView(text("Jarvis • respostas por voz e texto", 10f, MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        setContentView(root)

        addMessage("JARVIS", "Olá. Sou seu assistente. Configure o servidor no botão ⚙ e depois pode falar ou digitar comigo.", false)
        val savedEndpoint = getSharedPreferences(PREFS, MODE_PRIVATE).getString("endpoint", "").orEmpty()
        if (savedEndpoint.isNotBlank()) status.text = "  Servidor salvo • chave necessária nesta sessão"
    }

    private fun showConnectionSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val endpointField = EditText(this).apply {
            hint = "https://seu-servidor.onrender.com"
            setSingleLine(true)
            setTextColor(0xFF182433.toInt())
            setHintTextColor(0xFF607080.toInt())
            setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString("endpoint", ""))
        }
        val keyField = EditText(this).apply {
            hint = if (sessionKey.isBlank()) "Chave OPENJARVIS_API_KEY" else "Chave salva apenas nesta sessão"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setTextColor(0xFF182433.toInt())
            setHintTextColor(0xFF607080.toInt())
        }
        layout.addView(text("Endereço HTTPS do servidor", 12f, MUTED))
        layout.addView(endpointField)
        layout.addView(text("Chave do servidor (não é sua chave OpenAI)", 12f, MUTED).apply {
            setPadding(0, dp(12), 0, 0)
        })
        layout.addView(keyField)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Conexão do Jarvis")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = endpointField.text.toString().trim().trimEnd('/')
                val key = keyField.text.toString().trim()
                if (!url.startsWith("https://")) {
                    endpointField.error = "Informe um endereço HTTPS válido"
                    return@setOnClickListener
                }
                if (key.isNotBlank()) sessionKey = key
                if (sessionKey.isBlank()) {
                    keyField.error = "Informe a chave do servidor"
                    return@setOnClickListener
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("endpoint", url).apply()
                status.text = "  Servidor configurado • pronto para conversar"
                dialog.dismiss()
                Toast.makeText(this, "Conexão configurada nesta sessão.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun addMessage(who: String, body: String, fromUser: Boolean): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }
        row.addView(text(if (fromUser) "VOCÊ" else "JARVIS", 9f, if (fromUser) ACCENT else MUTED, true).apply {
            setPadding(dp(4), 0, dp(4), dp(4))
        })
        val bubble = TextView(this).apply {
            text = body
            textSize = 15f
            setTextColor(TEXT)
            setLineSpacing(dp(2).toFloat(), 1.0f)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(if (fromUser) USER_BUBBLE else PANEL, 17f)
            maxWidth = (resources.displayMetrics.widthPixels * 0.86f).toInt()
        }
        row.addView(bubble)
        messages.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return bubble
    }

    private fun setBusy(value: Boolean) {
        busy = value
        sendButton.isEnabled = !value
        sendButton.alpha = if (value) 0.45f else 1f
        micButton.isEnabled = !value
        status.text = if (value) "  Jarvis está respondendo…" else "  Conectado • pronto"
    }

    private fun sendMessage() {
        if (busy) return
        val userText = prompt.text.toString().trim()
        val base = getSharedPreferences(PREFS, MODE_PRIVATE).getString("endpoint", "").orEmpty()
        if (userText.isBlank()) return
        if (base.isBlank() || sessionKey.isBlank()) {
            showConnectionSettings()
            Toast.makeText(this, "Configure a conexão primeiro.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!base.startsWith("https://")) {
            Toast.makeText(this, "Por segurança, use HTTPS.", Toast.LENGTH_LONG).show()
            return
        }

        prompt.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(prompt.windowToken, 0)
        addMessage("VOCÊ", userText, true)
        history.put(JSONObject().put("role", "user").put("content", userText))
        val assistantBubble = addMessage("JARVIS", "Conectando…", false)
        setBusy(true)

        val requestMessages = JSONArray()
        val start = (history.length() - MAX_CONTEXT_MESSAGES).coerceAtLeast(0)
        for (i in start until history.length()) requestMessages.put(history.getJSONObject(i))
        val requestKey = sessionKey

        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$base/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 90_000
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Authorization", "Bearer $requestKey")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "text/event-stream")
                }
                val payload = JSONObject()
                    .put("model", DEFAULT_MODEL)
                    .put("messages", requestMessages)
                    .put("stream", true)
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IllegalStateException("HTTP $code" + if (detail.isBlank()) "" else ": " + detail.take(240))
                }

                val answer = StringBuilder()
                connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        if (!line.startsWith("data:")) return@forEach
                        val data = line.removePrefix("data:").trim()
                        if (data.isBlank() || data == "[DONE]") return@forEach
                        val chunk = try {
                            JSONObject(data).optJSONArray("choices")
                                ?.optJSONObject(0)?.optJSONObject("delta")
                                ?.optString("content").orEmpty()
                        } catch (_: Exception) { "" }
                        if (chunk.isNotEmpty()) {
                            answer.append(chunk)
                            val partial = answer.toString()
                            runOnUiThread { assistantBubble.text = partial; scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }
                        }
                    }
                }
                val finalAnswer = answer.toString().trim()
                if (finalAnswer.isBlank()) throw IllegalStateException("O servidor não enviou texto. Verifique se o modelo oferece streaming.")
                history.put(JSONObject().put("role", "assistant").put("content", finalAnswer))
                runOnUiThread {
                    assistantBubble.text = finalAnswer
                    setBusy(false)
                    tts?.speak(finalAnswer, TextToSpeech.QUEUE_FLUSH, null, "jarvis-reply")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    assistantBubble.text = "Não consegui concluir: " + (error.message ?: "falha de conexão") + ". Confira o servidor e tente novamente."
                    setBusy(false)
                    status.text = "  Falha • toque em ⚙ para verificar a conexão"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun startSpeechInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale com o Jarvis")
        }
        try {
            startActivityForResult(intent, SPEECH_REQUEST)
        } catch (_: Exception) {
            Toast.makeText(this, "Reconhecimento de voz indisponível neste aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startSpeechInput()
        } else if (requestCode == AUDIO_PERMISSION_REQUEST) {
            Toast.makeText(this, "Permissão de microfone não concedida.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Activity result API retained for this starter module")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { prompt.setText(spoken); sendMessage() }
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts?.language = Locale("pt", "BR")
    }

    override fun onDestroy() {
        executor.shutdownNow()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
