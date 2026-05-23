package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.InterestScore
import com.example.data.model.NewsArticle
import com.example.data.model.SavedArticle
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): NewsArticle?

    @Query("SELECT * FROM articles WHERE id = :id")
    fun getArticleFlowById(id: String): Flow<NewsArticle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Query("UPDATE articles SET aiSummary = :summary, aiKeyPoints = :keyPoints, aiCategory = :aiCategory WHERE id = :id")
    suspend fun updateArticleAiData(id: String, summary: String, keyPoints: String, aiCategory: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedArticle(savedArticle: SavedArticle)

    @Query("DELETE FROM saved_articles WHERE articleId = :id")
    suspend fun deleteSavedArticle(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_articles WHERE articleId = :id)")
    fun isArticleSaved(id: String): Flow<Boolean>

    @Query("SELECT a.* FROM articles a INNER JOIN saved_articles s ON a.id = s.articleId ORDER BY s.savedAt DESC")
    fun getSavedArticles(): Flow<List<NewsArticle>>

    // UserProfile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()

    // InterestScore
    @Query("SELECT * FROM interest_score ORDER BY score DESC")
    fun getTopInterests(): Flow<List<InterestScore>>

    @Query("SELECT * FROM interest_score WHERE category = :category")
    suspend fun getInterestScore(category: String): InterestScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestScore(score: InterestScore)
}
