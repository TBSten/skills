package jp.news.app.navigation

import androidx.compose.runtime.mutableStateListOf

/** アプリ全体で 1 つ。DeepLinkHandler や PushNotificationRouter からも使われている。 */
class Navigator {
    val backStack = mutableStateListOf<Route>(Home)

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
}
