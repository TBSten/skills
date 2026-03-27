package com.example.navigation3maintab.maintab

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * [NavEntry.metadata] に [mainTab] が設定されたエントリを
 * [MainTabScene] (Scaffold + NavigationBar) でラップして表示する [SceneStrategy]。
 *
 * タブなしの画面は null を返し、チェーン先の Strategy にフォールバックする。
 */
class MainTabSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>,
    ): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val tab = lastEntry.metadata[MAIN_TAB_KEY] as? MainTab ?: return null
        return MainTabScene(
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            currentTab = tab,
        )
    }

    companion object {
        fun mainTab(tab: MainTab): Map<String, Any> = mapOf(MAIN_TAB_KEY to tab)

        internal const val MAIN_TAB_KEY = "mainTab"
    }
}
