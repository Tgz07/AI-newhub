package com.example.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.InterestScore
import com.example.data.model.NewsArticle
import com.example.data.model.SavedArticle
import com.example.data.model.UserProfile

@Database(entities = [NewsArticle::class, SavedArticle::class, UserProfile::class, InterestScore::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
}
