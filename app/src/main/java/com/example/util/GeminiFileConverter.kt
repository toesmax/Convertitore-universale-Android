package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object GeminiFileConverter {
    private const val TAG = "GeminiFileConverter"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun convertFileWithAI(
        inputFile: File,
        outputFile: File,
        targetExtension: String,
        additionalPrompt: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Chiave API Gemini non configurata nel Pannello dei Segreti di AI Studio."))
            }

            val sourceText = inputFile.readText()
            val prompt = """
                Sei un assistente di conversione file avanzatissimo. Il tuo compito è convertire l'intero contenuto del file sorgente dall'estensione originale (.${inputFile.extension}) all'estensione di destinazione desiderata (.${targetExtension}).
                Esegui una conversione sintatticamente perfetta.
                
                IMPORTANTE: Restituisci ESCLUSIVAMENTE il contenuto grezzo e pulito del file convertito finale. Non inserire descrizioni, introduzioni o blocchi di codice markdown (NON usare i delimitatori ``` con il nome del linguaggio). Il tuo output deve essere immediatamente compilabile/interpretabile o visualizzabile per quell'estensione.
                
                Istruzioni aggiuntive dell'utente per la conversione:
                $additionalPrompt
                
                === CONTENUTO FILE SORGENTE (.${inputFile.extension}) ===
                $sourceText
            """.trimIndent()

            // Construct Gemini request body JSON via native JSONObject
            val partObj = JSONObject().put("text", prompt)
            val partsArr = JSONArray().put(partObj)
            val contentObj = JSONObject().put("parts", partsArr)
            val contentsArr = JSONArray().put(contentObj)
            val requestBodyJson = JSONObject().put("contents", contentsArr)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API error: $errorMsg")
                    return@withContext Result.failure(Exception("Errore API Gemini (${response.code}): $errorMsg"))
                }

                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Gemini API response body received")

                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Nessuna risposta generata da Gemini."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val responseContent = firstCandidate.optJSONObject("content")
                val responseParts = responseContent?.optJSONArray("parts")
                if (responseParts == null || responseParts.length() == 0) {
                    return@withContext Result.failure(Exception("Nessuna parte testuale trovata nella risposta di Gemini."))
                }

                var resultText = responseParts.getJSONObject(0).optString("text")
                if (resultText.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Il file restituito dall'AI è vuoto."))
                }

                // Defensive cleaning: in case Gemini ignores instructions and wraps output inside markdown backticks
                resultText = cleanMarkdownWrapper(resultText, targetExtension)

                if (outputFile.exists()) {
                    outputFile.delete()
                }
                outputFile.createNewFile()
                outputFile.writeText(resultText)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI file conversion", e)
            Result.failure(e)
        }
    }

    private fun cleanMarkdownWrapper(content: String, targetExtension: String): String {
        var trimmed = content.trim()
        
        // Remove starting backticks e.g. ```json or ```xml or ```yaml or ```
        if (trimmed.startsWith("```")) {
            val lines = trimmed.split("\n").toMutableList()
            if (lines.isNotEmpty() && lines.first().startsWith("```")) {
                lines.removeAt(0)
            }
            if (lines.isNotEmpty() && lines.last().endsWith("```")) {
                lines.removeAt(lines.size - 1)
            }
            trimmed = lines.joinToString("\n").trim()
        }
        
        return trimmed
    }
}
