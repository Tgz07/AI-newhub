package com.example.ai

import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GeminiClient
import com.example.api.GenerateContentRequest
import com.example.api.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiSummarizer {
    suspend fun summarizeArticle(title: String, contentToSummarize: String): AiSummaryResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }
        
        val prompt = """
            Please summarize the following news article briefly in Vietnamese (1-2 sentences).
            Also, provide 3 to 5 key bullet points.
            And classify it into ONE of these categories: Tin nóng, Thời sự, Thế giới, Kinh tế, Công nghệ, Giáo dục, Sức khỏe, Thể thao, Giải trí, Pháp luật, Đời sống, Khoa học, Xe, Bất động sản.
            
            Title: $title
            Content: $contentToSummarize
            
            Return format exactly like this:
            SUMMARY: <short summary in Vietnamese>
            KEY_POINTS: <bullet 1>|||<bullet 2>|||<bullet 3>
            CATEGORY: <Category Name>
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext null
            
            var summary = ""
            var keyPoints = ""
            var category = ""
            
            responseText.lines().forEach { line ->
                when {
                    line.startsWith("SUMMARY:") -> summary = line.removePrefix("SUMMARY:").trim()
                    line.startsWith("KEY_POINTS:") -> keyPoints = line.removePrefix("KEY_POINTS:").trim()
                    line.startsWith("CATEGORY:") -> category = line.removePrefix("CATEGORY:").trim()
                }
            }
            if (summary.isNotEmpty()) {
               return@withContext AiSummaryResult(summary, keyPoints, category)
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

data class AiSummaryResult(
    val summary: String,
    val keyPoints: String,
    val category: String
)
