package com.bittupandit.astraquantumai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** On-device Qwen3-0.6B runtime. Download starts only after an explicit user action. */
class OfflineAiEngine(context: Context) {
    private val modelDirectory = File(context.applicationContext.filesDir, "offline-models").apply { mkdirs() }
    private val modelFile = File(modelDirectory, MODEL_FILE)
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    val isModelInstalled: Boolean
        get() = modelFile.isFile && modelFile.length() in MIN_MODEL_BYTES..MAX_MODEL_BYTES

    suspend fun downloadModel(onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        if (isModelInstalled) {
            onProgress(100)
            return@withContext
        }
        val partial = File(modelDirectory, "$MODEL_FILE.part")
        partial.delete()
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 60000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ASTRA-QUANTUM-AI-PLUS/1.0")
        }
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "मॉडल डाउनलोड नहीं हो पाया · HTTP ${connection.responseCode}" }
            val declaredLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(1024 * 256)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_MODEL_BYTES) { "मॉडल फ़ाइल का आकार अपेक्षा से बड़ा है।" }
                        output.write(buffer, 0, count)
                        if (declaredLength > 0) onProgress(((total * 100L) / declaredLength).toInt().coerceIn(0, 99))
                    }
                    output.fd.sync()
                }
            }
            check(partial.length() in MIN_MODEL_BYTES..MAX_MODEL_BYTES) {
                "डाउनलोड अधूरा या गलत है (${partial.length()} bytes)। दोबारा प्रयास करें।"
            }
            check(partial.renameTo(modelFile)) { "मॉडल फ़ाइल सुरक्षित नहीं की जा सकी।" }
            onProgress(100)
        } catch (error: Exception) {
            partial.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    suspend fun initialize(): String = withContext(Dispatchers.IO) {
        check(isModelInstalled) { "पहले Offline AI पेज से मॉडल डाउनलोड करें।" }
        if (conversation != null) return@withContext "स्थानीय Qwen मॉडल तैयार है।"
        try {
            val gpuEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.GPU()))
            gpuEngine.initialize()
            engine = gpuEngine
            conversation = gpuEngine.createConversation()
            "Qwen3-0.6B तैयार · GPU"
        } catch (_: Throwable) {
            runCatching { engine?.close() }
            engine = null
            conversation = null
            val cpuEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.CPU()))
            cpuEngine.initialize()
            engine = cpuEngine
            conversation = cpuEngine.createConversation()
            "Qwen3-0.6B तैयार · CPU fallback"
        }
    }

    suspend fun answer(prompt: String): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "सवाल खाली है।" }
        if (conversation == null) initialize()
        val activeConversation = conversation ?: error("स्थानीय AI इंजन शुरू नहीं हो पाया।")
        val response = StringBuilder()
        activeConversation.sendMessageAsync(prompt).collect { message ->
            val chunk = message.toString()
            if (chunk.isNotBlank()) response.append(chunk)
        }
        response.toString().trim().ifBlank { "स्थानीय मॉडल से उत्तर नहीं मिला।" }
    }

    fun close() {
        runCatching { conversation?.close() }
        conversation = null
        runCatching { engine?.close() }
        engine = null
    }

    companion object {
        const val MODEL_NAME = "Qwen3-0.6B · 328 MiB"
        private const val MODEL_FILE = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        private const val MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        private const val MIN_MODEL_BYTES = 250L * 1024L * 1024L
        private const val MAX_MODEL_BYTES = 400L * 1024L * 1024L
    }
}
