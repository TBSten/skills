package jp.news.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey

@Serializable data object Home : Route
@Serializable data object Search : Route
@Serializable data object Notifications : Route
@Serializable data object Settings : Route
@Serializable data class ArticleDetail(val articleId: Long) : Route
