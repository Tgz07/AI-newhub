package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.ai.AiSummarizer
import com.example.api.RssService
import com.example.data.db.AppDatabase
import com.example.data.model.InterestScore
import com.example.data.model.NewsArticle
import com.example.data.model.SavedArticle
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jsoup.Jsoup
import java.util.UUID

class NewsRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "ai_news_hub_db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()
    
    private val newsDao = db.newsDao()
    private val rssService = RssService()
    private val aiSummarizer = AiSummarizer()

    val allArticles: Flow<List<NewsArticle>> = newsDao.getAllArticles()
    val savedArticles: Flow<List<NewsArticle>> = newsDao.getSavedArticles()
    
    val userProfile: Flow<UserProfile?> = newsDao.getUserProfile()
    val topInterests: Flow<List<InterestScore>> = newsDao.getTopInterests()

    val recommendedArticles: Flow<List<NewsArticle>> = newsDao.getAllArticles().combine(newsDao.getTopInterests()) { articles, interests ->
        val topCategories = interests.take(3).map { it.category.lowercase() }
        if (topCategories.isEmpty()) return@combine articles
        
        articles.sortedByDescending { article ->
            val aiCat = article.aiCategory?.lowercase() ?: ""
            if (topCategories.any { aiCat.contains(it) }) 1 else 0
        }
    }

    suspend fun increaseInterest(category: String?) {
        if (category.isNullOrEmpty()) return
        val current = newsDao.getInterestScore(category)
        val newScore = (current?.score ?: 0) + 1
        newsDao.insertInterestScore(InterestScore(category, newScore))
    }

    suspend fun saveUserProfile(name: String, email: String) {
        newsDao.saveUserProfile(UserProfile(email = email, name = name))
    }
    
    suspend fun logout() {
        newsDao.deleteUserProfile()
    }

    suspend fun fetchAndSummarizeUrl(url: String): NewsArticle? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val title = doc.title()
            val text = doc.body().text()
            // limit text to ~5000 chars to avoid prompt limits
            val contentToSummarize = if (text.length > 5000) text.substring(0, 5000) else text
            
            val aiResult = aiSummarizer.summarizeArticle(title, contentToSummarize) ?: return@withContext null
            
            val article = NewsArticle(
                id = url.hashCode().toString(),
                sourceName = doc.location().let { java.net.URI(it).host } ?: "Toàn cầu",
                title = title,
                originalUrl = url,
                thumbnailUrl = doc.select("meta[property=og:image]").firstOrNull()?.attr("content"),
                category = aiResult.category,
                publishedAt = System.currentTimeMillis(),
                collectedAt = System.currentTimeMillis(),
                isVerified = true,
                aiSummary = aiResult.summary,
                aiKeyPoints = aiResult.keyPoints,
                aiCategory = aiResult.category
            )
            newsDao.insertArticles(listOf(article))
            return@withContext article
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>> {
        return newsDao.getArticlesByCategory(category)
    }

    suspend fun refreshNews(rssUrl: String = "https://vnexpress.net/rss/tin-moi-nhat.rss", sourceName: String = "VNExpress") {
        val rssItems = rssService.fetchVnExpressRss(rssUrl)
        val newArticles = rssItems.map { item ->
            val cleanDescription = item.description.replace(Regex("<.*?>"), "").trim()
            NewsArticle(
                id = item.link.hashCode().toString(),
                sourceName = sourceName,
                title = item.title,
                originalUrl = item.link,
                thumbnailUrl = item.imageUrl ?: "https://picsum.photos/400/200?random=${item.link.hashCode()}",
                category = "Tin mới nhất",
                publishedAt = System.currentTimeMillis(), // We could parse pubDate but let's keep it simple
                collectedAt = System.currentTimeMillis(),
                isVerified = true,
                aiSummary = cleanDescription // Initial summary is just the RSS description
            )
        }
        newsDao.insertArticles(newArticles)
    }

    suspend fun summarizeArticleWithAi(articleId: String) {
        val article = newsDao.getArticleById(articleId) ?: return
        if (!article.aiKeyPoints.isNullOrEmpty()) return // Already summarized
        
        val contentToSummarize = article.aiSummary ?: article.title // Use description as base content
        val result = aiSummarizer.summarizeArticle(article.title, contentToSummarize)
        
        if (result != null) {
            newsDao.updateArticleAiData(
                id = article.id,
                summary = result.summary,
                keyPoints = result.keyPoints,
                aiCategory = result.category
            )
        }
    }

    fun isArticleSaved(id: String): Flow<Boolean> = newsDao.isArticleSaved(id)

    suspend fun toggleSaveArticle(articleId: String, isCurrentlySaved: Boolean) {
        if (isCurrentlySaved) {
            newsDao.deleteSavedArticle(articleId)
        } else {
            newsDao.insertSavedArticle(SavedArticle(articleId))
        }
    }

    fun getArticleFlowById(id: String): Flow<NewsArticle?> = newsDao.getArticleFlowById(id)
}
