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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
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
    private lateinit var endpoint: EditText
    private lateinit var apiKey: EditText
    private lateinit var prompt: EditText
    private lateinit var transcript: LinearLayout
    private lateinit var status: TextView
    private lateinit var sendButton: TextView
    private lateinit var micButton: TextView
    private var tts: TextToSpeech? = null
    private var apiBase = ""
    private var sessionKey = ""
    private var busy = false

    companion object {
        private const val SPEECH_REQUEST = 4101
        private const val AUDIO_PERMISSION_REQUEST = 4102
        private const val PREFS = "openjarvis_mobile"
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private const val BG = 0xFF090E16.toInt()
        private const val PANEL = 0xFF121C2A.toInt()
        private const val CYAN = 0xFF54D7F5.toInt()
        private const val MUTED = 0xFF8FA2B9.toInt()
        private const val WHITE = 0xFFEAF4FF.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        apiBase = getSharedPreferences(PREFS, MODE_PRIVATE).getString("endpoint", "") ?: ""
        tts = TextToSpeech(this, this)
        buildUi()
        if (apiBase.isBlank()) status.text = "CONFIGURAÇÃO NECESSÁRIA"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(BG)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(14))
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(text("J.A.R.V.I.S", 23f, CYAN, true))
        brand.addView(text("ASSISTENTE PESSOAL", 10f, MUTED, true))
        val settings = text("⚙", 25f, WHITE).apply {
            gravity = Gravity.CENTER
            background = rounded(PANEL, 16, 0xFF26374A.toInt())
            setPadding(dp(14), dp(7), dp(14), dp(7))
            contentDescription = "Configurações do servidor"
            setOnClickListener { showSettings() }
        }
        header.addView(brand)
        header.addView(settings)
        root.addView(header)

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0xFF101B28.toInt(), 14)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val dot = text("●", 11f, 0xFFFFC66D.toInt())
        status = text("DESCONECTADO • configure o servidor", 11f, MUTED, true).apply {
            setPadding(dp(8), 0, 0, 0)
        }
        statusRow.addView(dot)
        statusRow.addView(status)
        root.addView(statusRow)

        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, dp(10))
        }
        section.addView(text("CONVERSA", 11f, MUTED, true))
        section.addView(text("Como posso ajudar?", 21f, WHITE, true).apply { setPadding(0, dp(3), 0, 0) })
        root.addView(section)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(12))
        }
        transcript = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(transcript)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        if (apiBase.isBlank()) {
            addMessage("JARVIS", "Olá. Sou seu assistente pessoal. Para começar, abra as configurações e conecte seu servidor OpenJarvis.", false)
        } else {
            addMessage("JARVIS", "Conexão configurada. Envie uma mensagem para iniciar.", false)
        }

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(PANEL, 20, 0xFF26374A.toInt())
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        micButton = text("🎙", 19f, CYAN).apply {
            gravity = Gravity.CENTER
            background = rounded(0xFF1A2B3D.toInt(), 15)
            setPadding(dp(12), dp(11), dp(12), dp(11))
            setOnClickListener { startSpeechInput() }
        }
        prompt = EditText(this).apply {
            hint = "Escreva uma mensagem..."
            textSize = 15f
            setTextColor(WHITE)
            setHintTextColor(MUTED)
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(12), dp(8), dp(8), dp(8))
            background = null
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        sendButton = text("➤", 20f, BG, true).apply {
            gravity = Gravity.CENTER
            background = rounded(CYAN, 15)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { sendMessage() }
        }
        composer.addView(micButton)
        composer.addView(prompt, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(sendButton)
        root.addView(composer)
        root.addView(text("MICROFONE SOB SEU CONTROLE  •  CONEXÃO HTTPS", 9f, 0xFF60758D.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, 0)
        })
        setContentView(root)
    }

    private fun addMessage(who: String, message: String, user: Boolean) {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (user) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }
        wrap.addView(text(if (user) "VOCÊ" else who.uppercase(Locale.ROOT), 10f, if (user) CYAN else MUTED, true).apply {
            setPadding(dp(4), 0, dp(4), dp(5))
        })
        val bubble = text(message, 15f, WHITE).apply {
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (user) 0xFF15354A.toInt() else PANEL, 16)
            maxWidth = (resources.displayMetrics.widthPixels * 0.82f).toInt()
        }
        wrap.addView(bubble)
        transcript.addView(wrap)
        transcript.post {
            val parent = transcript.parent as? ScrollView
            parent?.smoothScrollTo(0, transcript.bottom)
        }
    }

    private fun showSettings() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun configField(hintText: String, value: String, secret: Boolean = false) =
            EditText(this).apply {
                hint = hintText
                setText(value)
                textSize = 14f
                setSingleLine(true)
                setTextColor(WHITE)
                setHintTextColor(MUTED)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = rounded(0xFF202C3B.toInt(), 12)
                if (secret) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        content.addView(text("URL HTTPS DO SERVIDOR", 10f, MUTED, true))
        endpoint = configField("https://seu-servico.onrender.com", apiBase)
        content.addView(endpoint, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        content.addView(text("CHAVE DA API", 10f, MUTED, true))
        apiKey = configField("Sua chave de API", sessionKey, true)
        content.addView(apiKey, LinearLayout.LayoutParams(-1, -2))
        AlertDialog.Builder(this)
            .setTitle("Conectar ao OpenJarvis")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val base = endpoint.text.toString().trim().trimEnd('/')
                val key = apiKey.text.toString().trim()
                if (!base.startsWith("https://") || key.isBlank()) {
                    Toast.makeText(this, "Informe uma URL HTTPS e uma chave válida.", Toast.LENGTH_LONG).show()
                } else {
                    apiBase = base
                    sessionKey = key
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("endpoint", base).apply()
                    status.text = "CONFIGURADO • pronto para conectar"
                    Toast.makeText(this, "Configuração salva nesta sessão.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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
        try { startActivityForResult(intent, SPEECH_REQUEST) }
        catch (_: Exception) { Toast.makeText(this, "Reconhecimento de voz indisponível.", Toast.LENGTH_LONG).show() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startSpeechInput()
        else if (requestCode == AUDIO_PERMISSION_REQUEST) Toast.makeText(this, "Permissão de microfone não concedida.", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Activity result API retained for starter compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { prompt.setText(spoken); sendMessage() }
        }
    }

    private fun sendMessage() {
        val message = prompt.text.toString().trim()
        if (busy) return
        if (message.isBlank()) return
        if (apiBase.isBlank() || sessionKey.isBlank()) {
            Toast.makeText(this, "Abra ⚙ e configure o servidor e a chave.", Toast.LENGTH_LONG).show()
            showSettings()
            return
        }
        prompt.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(prompt.windowToken, 0)
        addMessage("VOCÊ", message, true)
        history.put(JSONObject().put("role", "user").put("content", message))
        busy = true
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f
        status.text = "JARVIS ESTÁ PROCESSANDO…"
        executor.execute {
            try {
                val connection = (URL("$apiBase/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 90_000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $sessionKey")
                    setRequestProperty("Content-Type", "application/json")
                }
                val messages = JSONArray()
                for (i in 0 until history.length()) messages.put(history.getJSONObject(i))
                val payload = JSONObject().put("model", DEFAULT_MODEL).put("messages", messages).put("stream", false)
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Erro HTTP $code: $body")
                val answer = JSONObject(body).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content")
                if (answer.isBlank()) throw IllegalStateException("O servidor retornou uma resposta vazia.")
                history.put(JSONObject().put("role", "assistant").put("content", answer))
                runOnUiThread {
                    addMessage("JARVIS", answer, false)
                    status.text = "SESSÃO ATIVA • pronto"
                    busy = false
                    sendButton.isEnabled = true
                    sendButton.alpha = 1f
                    tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis-reply")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    status.text = "FALHA NA CONEXÃO"
                    addMessage("AVISO", error.message ?: "Não foi possível conectar ao servidor.", false)
                    busy = false
                    sendButton.isEnabled = true
                    sendButton.alpha = 1f
                }
            }
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
