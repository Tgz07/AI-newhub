package com.example.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NewsArticle
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: NewsRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _isFetchingUrl = MutableStateFlow(false)
    val isFetchingUrl = _isFetchingUrl.asStateFlow()

    val topInterests = repository.topInterests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults = _query.combine(repository.allArticles) { q, articles ->
        if (q.isBlank()) emptyList()
        else articles.filter { it.title.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
    
    fun fetchUrl(url: String, onResult: (String?) -> Unit) {
        if (!url.startsWith("http")) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            _isFetchingUrl.value = true
            val article = repository.fetchAndSummarizeUrl(url)
            _isFetchingUrl.value = false
            onResult(article?.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    navigateToDetail: (String) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isFetching by viewModel.isFetchingUrl.collectAsState()
    val topInterests by viewModel.topInterests.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm kiếm bài viết hoặc dán link URL") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = query.isBlank() && topInterests.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Từ khóa nổi bật cho bạn", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(topInterests) { interest ->
                        SuggestionChip(
                            onClick = { viewModel.updateQuery(interest.category) },
                            label = { Text(interest.category) }
                        )
                    }
                }
            }
        }

        if (query.startsWith("http")) {
            Button(
                onClick = { 
                    viewModel.fetchUrl(query) { id -> 
                        if (id != null) navigateToDetail(id) 
                    } 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetching
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Phân tích URL toàn cầu bằng AI")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(results) { article ->
                ListItem(
                    headlineContent = { Text(article.title, maxLines = 2) },
                    supportingContent = { Text(article.sourceName) },
                    modifier = Modifier.clickable { navigateToDetail(article.id) }
                )
                HorizontalDivider()
            }
        }
    }
}
