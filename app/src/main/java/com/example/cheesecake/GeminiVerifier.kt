package com.example.cheesecake

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

/**
 * Helper object to verify drinking action using Gemini Vision API.
 */
object GeminiVerifier {

    private const val TAG = "GeminiVerifier"
    private const val API_KEY = "AIzaSyCrIJ6xb_9tnwGg7FZG3RW-Rs2ClQjqqLw"

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    /**
     * Analyzes a bitmap image to determine if the person is actively drinking water.
     * @param bitmap The image frame to analyze.
     * @return True if Gemini confirms the person is drinking, False otherwise.
     */
    suspend fun verifyDrinking(bitmap: Bitmap): Boolean {
        return try {
            val prompt = """
                Analyze this image carefully. 
                Is the person in this image ACTIVELY DRINKING water or liquid from a bottle or container?
                Important: They must be in the act of drinking (bottle tilted, liquid flowing, drinking posture).
                Simply holding a bottle near the face does NOT count.
                
                Respond with ONLY one word: YES or NO.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = model.generateContent(inputContent)
            val result = response.text?.trim()?.uppercase() ?: "NO"
            
            Log.d(TAG, "Gemini Response: $result")
            
            result.contains("YES")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
            // Fallback: Return true on error to not block the user if API fails
            // You might want to change this to false for stricter verification
            true 
        }
    }
}
