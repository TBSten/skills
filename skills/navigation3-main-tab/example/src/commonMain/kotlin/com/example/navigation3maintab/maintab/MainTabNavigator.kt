package com.example.navigation3maintab.maintab

interface MainTabNavigator {
    val currentTab: MainTab?
    fun switchTab(tab: MainTab)
}
