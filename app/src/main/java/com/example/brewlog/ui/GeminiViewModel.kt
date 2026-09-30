package com.example.brewlog.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brewlog.BuildConfig
import com.example.brewlog.data.MachineScanResult
import com.example.brewlog.data.ScanResult
import com.example.brewlog.data.SettingsRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Locale

class GeminiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private suspend fun getGenerativeModel(): GenerativeModel {
        val userKey = repository.geminiApiKey.first().trim()
        val buildKey = BuildConfig.GEMINI_API_KEY
        val apiKey = when {
            SettingsRepository.isRealApiKey(userKey) -> userKey
            SettingsRepository.isRealApiKey(buildKey) -> buildKey
            else -> ""
        }
        return GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = apiKey,
        )
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _machineScanResult = MutableStateFlow<MachineScanResult?>(null)
    val machineScanResult = _machineScanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun scanLabel(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _scanResult.value = null

            try {
                val prompt = """
                    You are a coffee expert. Analyze this image of a coffee bag label and extract details in a structured JSON format.
                    
                    Required Fields:
                    - "coffeeName": The name of the coffee/blend.
                    - "roaster": The roasting company.

                    Optional Roast:
                    - "roastLevel": "Light", "Medium", or "Dark". Only set if clearly identifiable.
                    - "roastDate": The roast date formatted as "YYYY-MM-DD" if explicitly visible or stamped on the label (e.g. "2025-02-10"), otherwise empty string "".

                    Optional Sensory (Only set "hasSensoryProfile": true if sensory notes like acidity, sweetness etc are explicitly described):
                    - "hasSensoryProfile": true/false
                    - "sweetness", "acidity", "body", "bitterness": Map descriptions to 1-5. Default is 3.

                    Optional Flavors (Only set "hasFlavorTags": true if flavor notes are explicitly mentioned):
                    - "hasFlavorTags": true/false
                    - "flavorTags": List of matching tags from: [Chocolate, Nutty, Fruity, Floral, Caramel, Berry, Citrus, Spices, Earthy, Smoke].

                    Optional Blend Settings (Only set "hasBlendSettings": true if specific bean varieties/percentages are mentioned):
                    - "hasBlendSettings": true/false
                    - "arabicaPercentage", "robustaPercentage", "excelsaPercentage", "libericaPercentage": Percent values (0-100).

                    Return ONLY the JSON object.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val generativeModel = getGenerativeModel()
                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
                
                val jsonContent = extractJsonContent(responseText)

                val result = json.decodeFromString<ScanResult>(jsonContent)
                _scanResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = parseUserFriendlyErrorMessage(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractJsonContent(rawText: String): String {
        var text = when {
            rawText.contains("```json") -> rawText.substringAfter("```json").substringBefore("```").trim()
            rawText.contains("```") -> rawText.substringAfter("```").substringBeforeLast("```").trim()
            else -> rawText
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text
    }

    private fun parseUserFriendlyErrorMessage(rawMessage: String?): String {
        if (rawMessage.isNullOrBlank()) {
            return if (Locale.getDefault().language == "de") "Unbekannter KI-Fehler." else "Unknown AI error."
        }

        val isDe = Locale.getDefault().language == "de"

        if (rawMessage.contains("429") || rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || rawMessage.contains("quota", ignoreCase = true)) {
            return if (isDe) {
                "Die KI ist aktuell sehr stark ausgelastet (Limit erreicht). Bitte versuche es in wenigen Minuten erneut."
            } else {
                "The AI service is currently at capacity (quota limit reached). Please try again in a few minutes."
            }
        }

        if (rawMessage.contains("404") || rawMessage.contains("NOT_FOUND", ignoreCase = true)) {
            return if (isDe) {
                "KI-Modell nicht gefunden (404). Bitte überprüfe deinen Gemini API-Key in den Einstellungen."
            } else {
                "AI model not found (404). Please check your Gemini API key in Settings."
            }
        }

        if (rawMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
            rawMessage.contains("API key not valid", ignoreCase = true) ||
            rawMessage.contains("Invalid authentication credentials", ignoreCase = true) ||
            rawMessage.contains("OAuth 2", ignoreCase = true) ||
            rawMessage.contains("authentication credential", ignoreCase = true) ||
            rawMessage.contains("UNAUTHENTICATED", ignoreCase = true) ||
            rawMessage.contains("PERMISSION_DENIED", ignoreCase = true)
        ) {
            return if (isDe) {
                "Ungültiger API-Key. Bitte überprüfe deinen Gemini Key in den Einstellungen."
            } else {
                "Invalid API key. Please check your Gemini Key in Settings."
            }
        }

        // Try extracting the "message" field if JSON is present
        try {
            if (rawMessage.contains("\"message\"")) {
                val extractedMessage = rawMessage
                    .substringAfter("\"message\":")
                    .substringAfter("\"")
                    .substringBefore("\"")
                    .trim()
                if (extractedMessage.isNotBlank()) {
                    return extractedMessage
                }
            }
        } catch (_: Exception) {}

        return rawMessage.take(150)
    }

    private val _generatedGuide = MutableStateFlow<String?>(null)
    val generatedGuide = _generatedGuide.asStateFlow()

    fun clearResult() {
        _scanResult.value = null
        _machineScanResult.value = null
        _generatedGuide.value = null
    }

    fun clearGeneratedGuide() {
        _generatedGuide.value = null
    }

    fun generateMaintenanceGuide(brand: String, model: String, taskType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _generatedGuide.value = null

            try {
                val language = Locale.getDefault().language
                val langInstruction = if (language == "de") {
                    "Antworte vollständig auf Deutsch."
                } else {
                    "Respond entirely in English."
                }

                val prompt = """
                    You are a professional espresso machine maintenance technician.
                    Provide a detailed, clear, step-by-step maintenance guide for a home barista for the following:
                    Espresso Machine Brand: $brand
                    Espresso Machine Model: $model
                    Maintenance Task: $taskType (Water Filter / Descaling / Group Backflushing)

                    Structure & Formatting Rules:
                    - Divide the process into 6 to 10 small, clear, numbered steps (1., 2., 3., etc.).
                    - Keep each step short (1-2 sentences max) so it is easy to follow while performing the task.
                    - Tailor the steps specifically to this machine's architecture (e.g. thermoblock vs dual boiler vs E61 vs solenoid valve).
                    
                    $langInstruction
                    Do NOT include markdown headers (like # or ##) or unnecessary intro fluff. Start directly with Step 1.
                """.trimIndent()

                val inputContent = content { text(prompt) }
                val generativeModel = getGenerativeModel()
                val response = generativeModel.generateContent(inputContent)
                val text = response.text?.trim() ?: throw Exception("Empty response")
                _generatedGuide.value = text
            } catch (e: Exception) {
                _errorMessage.value = parseUserFriendlyErrorMessage(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchMachineInfo(brand: String, model: String) {
        Log.d("GeminiViewModel", "Fetching info for $brand $model")
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _machineScanResult.value = null

            try {
                val prompt = """
                    You are a coffee equipment expert. Analyze if the following espresso machine exists:
                    Brand: $brand
                    Model: $model

                    If the machine is not real or the brand/model names are too short/vague (like "s" or "test"), set "machineFound" to false.

                    If found, provide technical details in JSON:
                    - "machineFound": true.
                    - "portafilterDiameter": Number (e.g., 58.0, 54.0).
                    - "hasIntegratedGrinder": Boolean.
                    - "hasSteamWand": Boolean.
                    - "waterFilterMaxDays": Manufacturer recommended max days (e.g. 90).
                    - "descaleMaxDays": Manufacturer recommended max days (e.g. 180).
                    - "backflushMaxDays": Manufacturer recommended max days (e.g. 30).
                    - "waterFilterLimitCycles": Cycles limit (cups) if applicable, else 0.
                    - "descaleLimitCycles": Cycles limit (cups) if applicable, else 0.
                    - "backflushLimitCycles": Cycles limit (cups) if applicable, else 0.

                    Return ONLY the JSON object.
                """.trimIndent()

                val inputContent = content {
                    text(prompt)
                }

                val generativeModel = getGenerativeModel()
                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
                Log.d("GeminiViewModel", "AI Raw Response: $responseText")

                val jsonContent = extractJsonContent(responseText)

                val result = json.decodeFromString<MachineScanResult>(jsonContent)
                Log.d("GeminiViewModel", "Parsed Result: $result")
                _machineScanResult.value = result
            } catch (e: Exception) {
                Log.e("GeminiViewModel", "Error fetching machine info", e)
                _errorMessage.value = parseUserFriendlyErrorMessage(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
