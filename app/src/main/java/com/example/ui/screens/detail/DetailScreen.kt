package com.example.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.data.model.NewsArticle
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: NewsRepository,
    private val articleId: String
) : ViewModel() {

    val article: StateFlow<NewsArticle?> = repository.getArticleFlowById(articleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isSaved: StateFlow<Boolean> = repository.isArticleSaved(articleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repository.summarizeArticleWithAi(articleId)
            val a = repository.getArticleFlowById(articleId).firstOrNull()
            if (a != null) {
                repository.increaseInterest(a.aiCategory ?: a.category)
            }
        }
    }

    fun toggleSave() {
        viewModelScope.launch {
            repository.toggleSaveArticle(articleId, isSaved.value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit
) {
    val article by viewModel.article.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article?.sourceName ?: "Chi tiết") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSave() }) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save Article",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (article != null) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article!!.originalUrl))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (article == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                if (!article!!.thumbnailUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = article!!.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = article!!.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "TÓM TẮT BỞI AI",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = article!!.aiSummary ?: "Đang xử lý...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (article!!.aiKeyPoints != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Điểm chính:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val points = article!!.aiKeyPoints!!.split("|||")
                                points.forEach { point ->
                                    if (point.isNotBlank()) {
                                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Surface(
                                                modifier = Modifier.padding(top = 6.dp, end = 8.dp).size(6.dp),
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {}
                                            Text(
                                                text = point.trim(),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article!!.originalUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đọc bài gốc trên ${article!!.sourceName}")
                    }
                }
            }
        }
    }
}
