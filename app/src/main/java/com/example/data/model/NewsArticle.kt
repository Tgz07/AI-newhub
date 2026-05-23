package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "articles")
@Serializable
data class NewsArticle(
    @PrimaryKey val id: String, // from URL/hash
    val sourceName: String,
    val title: String,
    val originalUrl: String,
    val thumbnailUrl: String?,
    val category: String,
    val publishedAt: Long,
    val collectedAt: Long,
    val aiSummary: String? = null,
    val aiKeyPoints: String? = null, // JSON array string or comma separated
    val aiCategory: String? = null,
    val isVerified: Boolean = false
)

@Entity(tableName = "saved_articles")
data class SavedArticle(
    @PrimaryKey val articleId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "local_user",
    val email: String,
    val name: String,
    val isLoggedIn: Boolean = true
)

@Entity(tableName = "interest_score")
data class InterestScore(
    @PrimaryKey val category: String,
    val score: Int
)
