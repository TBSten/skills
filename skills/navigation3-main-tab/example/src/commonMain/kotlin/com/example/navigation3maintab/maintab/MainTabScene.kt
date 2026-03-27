package com.example.navigation3maintab.maintab

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene

/**
 * [MainTabSceneStrategy] が生成する [Scene]。
 * エントリのコンテンツを MainTabScreen (Scaffold + NavigationBar) でラップして描画する。
 *
 * [key] を定数にすることで、タブ切り替え時に Scene が破棄・再生成されず、
 * Scaffold + NavigationBar が維持されたまま中身だけが切り替わる。
 */
internal data class MainTabScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val key: Any = MainTabScene,
    private val currentTab: MainTab,
) : Scene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        MainTabScreen(currentTab = currentTab) {
            entry.Content()
        }
    }

    companion object
}
