package com.example.navigation3maintab.navigation

import androidx.compose.runtime.mutableStateListOf
import com.example.navigation3maintab.maintab.MainTab
import com.example.navigation3maintab.maintab.MainTabNavigator

interface AppNavigator : MainTabNavigator {
    val backstack: List<Screen>
    fun onBack()
}

class AppNavigatorImpl : AppNavigator {
    override val backstack = mutableStateListOf<Screen>(Home)

    override val currentTab: MainTab?
        get() = backstack.lastOrNull()?.mainTabOrNull

    override fun switchTab(tab: MainTab) {
        val screen = tab.screen
        // Replace the last tab screen in-place (not push)
        val lastTabIndex = backstack.indexOfLast { it.mainTabOrNull != null }
        if (lastTabIndex >= 0) {
            backstack[lastTabIndex] = screen
        } else {
            backstack.add(screen)
        }
    }

    override fun onBack() {
        if (backstack.size >= 2) backstack.removeLastOrNull()
    }
}
