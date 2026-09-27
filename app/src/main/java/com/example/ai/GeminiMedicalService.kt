package com.example.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ClassificationItem
import com.example.data.model.ClinicalPoint
import com.example.data.model.ExamEmphasisItem
import com.example.data.model.ManagementItem
import com.example.data.model.MedicalLectureNote
import com.example.data.model.NumberDoseItem
import com.example.data.model.QuestionAnswerItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

class GeminiMedicalService(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val defaultModel = "gemini-3.5-flash"

    /**
     * Resolves the active API key:
     * 1. User preferences override (configured in app settings)
     * 2. BuildConfig.GEMINI_API_KEY
     */
    fun getEffectiveApiKey(): String {
        val prefs = context.getSharedPreferences("soundscript_settings", Context.MODE_PRIVATE)
        val userKey = prefs.getString("custom_gemini_api_key", null)?.trim()
        if (!userKey.isNullOrEmpty()) {
            return userKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    suspend fun generateNotesFromAudio(
        audioFile: File,
        lectureTitleHint: String = "",
        specialtyHint: String = ""
    ): Result<MedicalLectureNote> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your Gemini API key in Settings or AI Studio Secrets.")
            )
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Data = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = when {
                audioFile.name.endsWith(".mp3", true) -> "audio/mp3"
                audioFile.name.endsWith(".wav", true) -> "audio/wav"
                audioFile.name.endsWith(".aac", true) -> "audio/aac"
                else -> "audio/mp4"
            }

            val requestJson = buildGeminiRequest(
                audioData = base64Data,
                audioMimeType = mimeType,
                textPrompt = "Analyze this medical lecture recording. Transcribe and extract comprehensive, high-yield English-only medical study revision notes according to the specified instructions. ${if (lectureTitleHint.isNotEmpty()) "Context: $lectureTitleHint ($specialtyHint)" else ""}"
            )

            callGeminiApi(requestJson, apiKey, audioFile.length() / 16000)
        } catch (e: Exception) {
            Log.e("GeminiMedicalService", "Audio notes generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateNotesFromText(
        transcriptOrNotes: String,
        lectureTitleHint: String = "",
        specialtyHint: String = ""
    ): Result<MedicalLectureNote> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your Gemini API key in Settings or AI Studio Secrets.")
            )
        }

        try {
            val requestJson = buildGeminiRequest(
                audioData = null,
                audioMimeType = null,
                textPrompt = """
                    Here is the medical lecture content/transcript:
                    ---
                    $transcriptOrNotes
                    ---
                    Analyze this medical lecture thoroughly and extract structured English-only study revision notes. ${if (lectureTitleHint.isNotEmpty()) "Title/Topic: $lectureTitleHint. Specialty: $specialtyHint." else ""}
                """.trimIndent()
            )

            callGeminiApi(requestJson, apiKey, 0)
        } catch (e: Exception) {
            Log.e("GeminiMedicalService", "Text notes generation failed", e)
            Result.failure(e)
        }
    }

    private fun buildGeminiRequest(
        audioData: String?,
        audioMimeType: String?,
        textPrompt: String
    ): JSONObject {
        val partsArray = JSONArray()

        if (audioData != null && audioMimeType != null) {
            val inlineDataObj = JSONObject().apply {
                put("mimeType", audioMimeType)
                put("data", audioData)
            }
            partsArray.put(JSONObject().apply { put("inlineData", inlineDataObj) })
        }

        partsArray.put(JSONObject().apply { put("text", textPrompt) })

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply { put("parts", partsArray) })
        }

        val systemInstruction = """
            You are SoundScript, an elite medical education scribe and revision assistant for medical students and clinicians.
            Your job is to convert spoken medical lectures into clean, rigorous, high-yield English-only revision notes.
            
            MANDATORY GUIDELINES:
            1. Output language: English only, regardless of the input language spoken in the lecture.
            2. Structured sections:
               - title: Clear medical topic name (e.g., 'Cardiology: Acute Coronary Syndromes').
               - specialty: Medical specialty (Cardiology, Neurology, Pharmacology, Pulmonology, Pediatrics, Surgery, etc.).
               - summary: Comprehensive 2-3 paragraph clinical overview.
               - coreConcepts: Fundamental pathophysiology, mechanisms of action, and anatomical principles.
               - importantPoints: Essential lecture takeaways.
               - classifications: Structured staging, grading, and criteria systems (e.g. NYHA, Killip, Wells, Glasgow).
               - clinicalPoints: Symptoms, signs, diagnostic criteria, lab cutoffs, imaging findings, and gold standard tests.
               - management: First-line, second-line, acute emergency measures, lifestyle, and contraindications.
               - numbersAndDoses: STRICT RULE: Report exact numerical values and dosages taught by the lecturer. If the lecturer was ambiguous or unclear about a dose or cutoff, explicitly set isUnclear: true and note '[unclear]' in the context. Never invent clinical dosages!
               - examEmphasis: High-yield board exam pearls, classic buzzwords, common question traps, and starred concepts.
               - questionAnswers: Preserve any question-and-answer exchanges or hypothetical questions posed by the lecturer with detailed answers.
               - highestYieldPoints: 4 to 6 rapid-fire high-yield bullet points for quick review right before an exam.
            3. Privacy & Raw Transcript Rule: DO NOT output or echo raw speech transcript. Output ONLY the structured study notes in clean JSON.
        """.trimIndent()

        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("temperature", 0.2)
        }

        return JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", generationConfig)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }
    }

    private fun callGeminiApi(
        requestJson: JSONObject,
        apiKey: String,
        estimatedDurationSec: Long
    ): Result<MedicalLectureNote> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            Log.e("GeminiMedicalService", "API error: ${response.code} $responseBody")
            val errorMessage = try {
                val errorObj = JSONObject(responseBody).getJSONObject("error")
                errorObj.getString("message")
            } catch (_: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            return Result.failure(IllegalStateException("Gemini API Error: $errorMessage"))
        }

        return try {
            val root = JSONObject(responseBody)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text")

            val parsedNote = parseMedicalNoteJson(rawText, estimatedDurationSec.toInt())
            Result.success(parsedNote)
        } catch (e: Exception) {
            Log.e("GeminiMedicalService", "Failed to parse Gemini response JSON", e)
            Result.failure(IllegalStateException("Failed to parse medical notes from Gemini response: ${e.message}"))
        }
    }

    private fun parseMedicalNoteJson(jsonString: String, durationSec: Int): MedicalLectureNote {
        // Strip markdown code fences if model enclosed them
        val cleaned = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleaned)

        val title = obj.optString("title", "Medical Lecture Revision Notes")
        val specialty = obj.optString("specialty", "Internal Medicine")
        val summary = obj.optString("summary", "")

        val coreConcepts = jsonArrayToStringList(obj.optJSONArray("coreConcepts"))
        val importantPoints = jsonArrayToStringList(obj.optJSONArray("importantPoints"))
        val highestYieldPoints = jsonArrayToStringList(obj.optJSONArray("highestYieldPoints"))

        val classifications = mutableListOf<ClassificationItem>()
        obj.optJSONArray("classifications")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                classifications.add(
                    ClassificationItem(
                        title = item.optString("title", ""),
                        category = item.optString("category", ""),
                        criteria = jsonArrayToStringList(item.optJSONArray("criteria"))
                    )
                )
            }
        }

        val clinicalPoints = mutableListOf<ClinicalPoint>()
        obj.optJSONArray("clinicalPoints")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                clinicalPoints.add(
                    ClinicalPoint(
                        category = item.optString("category", "General"),
                        description = item.optString("description", ""),
                        isHighYield = item.optBoolean("isHighYield", true)
                    )
                )
            }
        }

        val management = mutableListOf<ManagementItem>()
        obj.optJSONArray("management")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                management.add(
                    ManagementItem(
                        line = item.optString("line", "First-Line"),
                        intervention = item.optString("intervention", ""),
                        rationale = item.optString("rationale", "")
                    )
                )
            }
        }

        val numbersAndDoses = mutableListOf<NumberDoseItem>()
        obj.optJSONArray("numbersAndDoses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                numbersAndDoses.add(
                    NumberDoseItem(
                        item = item.optString("item", ""),
                        exactValue = item.optString("exactValue", ""),
                        context = item.optString("context", ""),
                        isUnclear = item.optBoolean("isUnclear", false)
                    )
                )
            }
        }

        val examEmphasis = mutableListOf<ExamEmphasisItem>()
        obj.optJSONArray("examEmphasis")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                examEmphasis.add(
                    ExamEmphasisItem(
                        topic = item.optString("topic", ""),
                        buzzword = item.optString("buzzword", ""),
                        pearl = item.optString("pearl", ""),
                        trapOrWarning = item.optString("trapOrWarning", "")
                    )
                )
            }
        }

        val questionAnswers = mutableListOf<QuestionAnswerItem>()
        obj.optJSONArray("questionAnswers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                questionAnswers.add(
                    QuestionAnswerItem(
                        question = item.optString("question", ""),
                        answer = item.optString("answer", ""),
                        lecturerNote = item.optString("lecturerNote", "")
                    )
                )
            }
        }

        return MedicalLectureNote(
            id = 0,
            title = title,
            specialty = specialty,
            createdAt = System.currentTimeMillis(),
            audioDurationSeconds = durationSec,
            summary = summary,
            coreConcepts = coreConcepts,
            importantPoints = importantPoints,
            classifications = classifications,
            clinicalPoints = clinicalPoints,
            management = management,
            numbersAndDoses = numbersAndDoses,
            examEmphasis = examEmphasis,
            questionAnswers = questionAnswers,
            highestYieldPoints = highestYieldPoints,
            isBookmarked = false
        )
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }
}
