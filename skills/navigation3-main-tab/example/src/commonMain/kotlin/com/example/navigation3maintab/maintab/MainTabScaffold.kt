package com.example.navigation3maintab.maintab

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun MainTabScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            MainTabNavigationBar(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
        },
        content = content,
    )
}

@Composable
private fun MainTabNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

// Customize icons and labels for your project
private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.Home -> Icons.Default.Home
        MainTab.ChatList -> Icons.Default.ChatBubble
        MainTab.MyPage -> Icons.Default.Person
    }

private val MainTab.label: String
    get() = when (this) {
        MainTab.Home -> "Home"
        MainTab.ChatList -> "Chat"
        MainTab.MyPage -> "My Page"
    }
