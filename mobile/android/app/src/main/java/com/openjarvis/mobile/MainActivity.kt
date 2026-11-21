package com.openjarvis.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private lateinit var transcript: TextView
    private lateinit var status: TextView
    private lateinit var sendButton: Button
    private var tts: TextToSpeech? = null

    companion object {
        private const val SPEECH_REQUEST = 4101
        private const val AUDIO_PERMISSION_REQUEST = 4102
        private const val PREFS = "openjarvis_mobile"
        private const val DEFAULT_MODEL = "gpt-4o-mini"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 20)
            setBackgroundColor(0xFF101318.toInt())
        }
        fun label(value: String) = TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFFB8C4D6.toInt())
            setPadding(0, 10, 0, 5)
        }
        fun field(hintText: String, value: String = "", secret: Boolean = false) =
            EditText(this).apply {
                hint = hintText
                setText(value)
                textSize = 15f
                setSingleLine(true)
                setTextColor(0xFFFFFFFF.toInt())
                setHintTextColor(0xFF8190A5.toInt())
                setPadding(14, 12, 14, 12)
                setBackgroundColor(0xFF202833.toInt())
                if (secret) inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        root.addView(TextView(this).apply {
            text = "JARVIS"
            textSize = 28f
            setTextColor(0xFF72D6FF.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(label("API HTTPS do seu OpenJarvis"))
        endpoint = field("https://seu-servico.onrender.com")
        endpoint.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString("endpoint", ""))
        root.addView(endpoint, matchWidth())
        root.addView(label("Chave da API (fica apenas nesta sessão)"))
        apiKey = field("OPENJARVIS_API_KEY", secret = true)
        root.addView(apiKey, matchWidth())

        status = label("Desconectado • configure a URL e a chave")
        root.addView(status)

        val historyScroll = ScrollView(this)
        transcript = TextView(this).apply {
            text = "Olá. Configure seu servidor para conversar com o Jarvis."
            textSize = 16f
            setTextColor(0xFFE8EEF7.toInt())
            setPadding(4, 12, 4, 12)
        }
        historyScroll.addView(transcript)
        root.addView(historyScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        prompt = field("Fale ou digite sua mensagem")
        root.addView(prompt, matchWidth())

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mic = Button(this).apply {
            text = "🎙 Falar"
            setOnClickListener { startSpeechInput() }
        }
        sendButton = Button(this).apply {
            text = "Enviar"
            setOnClickListener { sendMessage() }
        }
        actions.addView(mic, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(sendButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)
        setContentView(root)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun startSpeechInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST
            )
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

    @Deprecated("Activity result API kept minimal for compatibility with the starter module")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                prompt.setText(spoken)
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        val text = prompt.text.toString().trim()
        val base = endpoint.text.toString().trim().trimEnd('/')
        val key = apiKey.text.toString().trim()
        if (text.isBlank() || base.isBlank() || key.isBlank()) {
            Toast.makeText(this, "Informe a URL, a chave e a mensagem.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!base.startsWith("https://")) {
            Toast.makeText(this, "Use uma URL HTTPS segura.", Toast.LENGTH_LONG).show()
            return
        }
        prompt.setText("")
        append("Você", text)
        history.put(JSONObject().put("role", "user").put("content", text))
        sendButton.isEnabled = false
        status.text = "Jarvis está pensando…"
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("endpoint", base).apply()

        executor.execute {
            try {
                val connection = (URL("$base/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 90_000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                }
                val messages = JSONArray()
                for (i in 0 until history.length()) messages.put(history.getJSONObject(i))
                val payload = JSONObject()
                    .put("model", DEFAULT_MODEL)
                    .put("messages", messages)
                    .put("stream", false)
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Servidor respondeu HTTP $code: $body")
                val answer = JSONObject(body).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").optString("content")
                if (answer.isBlank()) throw IllegalStateException("O servidor retornou uma resposta vazia.")
                history.put(JSONObject().put("role", "assistant").put("content", answer))
                runOnUiThread {
                    append("JARVIS", answer)
                    status.text = "Conectado • pronto"
                    sendButton.isEnabled = true
                    tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis-reply")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    status.text = "Falha na conexão"
                    append("ERRO", error.message ?: "Não foi possível conectar.")
                    sendButton.isEnabled = true
                }
            }
        }
    }

    private fun append(who: String, message: String) {
        transcript.append("\n\n$who\n$message")
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
