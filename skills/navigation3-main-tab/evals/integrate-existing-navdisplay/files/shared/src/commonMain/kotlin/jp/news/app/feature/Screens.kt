package jp.news.app.feature

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable fun HomeScreen(onArticleClick: (Long) -> Unit) { Text("Home") }
@Composable fun SearchScreen(onArticleClick: (Long) -> Unit) { Text("Search") }
@Composable fun NotificationsScreen() { Text("Notifications") }
@Composable fun SettingsScreen(onBack: () -> Unit) { Text("Settings") }
@Composable fun ArticleDetailScreen(articleId: Long, onBack: () -> Unit) { Text("Article $articleId") }
