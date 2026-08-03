package com.example.brewlog.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brewlog.data.ScanResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class GeminiViewModel : ViewModel() {

    // ⚠️ IMPORTANT: Replace this with your actual Gemini API Key from Google AI Studio.
    // Obtain one for free at https://aistudio.google.com/
    private val apiKey = "YOUR_GEMINI_API_KEY"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.1-flash-lite",
        apiKey = apiKey
    )

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
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

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
                
                // Extraction logic for JSON inside markdown blocks or raw
                val jsonContent = when {
                    responseText.contains("```json") -> responseText.substringAfter("```json").substringBefore("```").trim()
                    responseText.contains("```") -> responseText.substringAfter("```").substringBeforeLast("```").trim()
                    else -> responseText
                }

                val result = json.decodeFromString<ScanResult>(jsonContent)
                _scanResult.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.message?.contains("404") == true) {
                    _errorMessage.value = "AI Error (404): Model not found. Please check your API Key and billing status in AI Studio."
                } else {
                    _errorMessage.value = "Failed to scan label: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResult() {
        _scanResult.value = null
    }
}
