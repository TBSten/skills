package jp.news.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import jp.news.app.feature.ArticleDetailScreen
import jp.news.app.feature.HomeScreen
import jp.news.app.feature.NotificationsScreen
import jp.news.app.feature.SearchScreen
import jp.news.app.feature.SettingsScreen

@Composable
fun AppNavHost(navigator: Navigator) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> { HomeScreen(onArticleClick = { navigator.navigate(ArticleDetail(it)) }) }
            entry<Search> { SearchScreen(onArticleClick = { navigator.navigate(ArticleDetail(it)) }) }
            entry<Notifications> { NotificationsScreen() }
            entry<Settings> { SettingsScreen(onBack = navigator::goBack) }
            entry<ArticleDetail> { route -> ArticleDetailScreen(route.articleId, onBack = navigator::goBack) }
        },
    )
}
