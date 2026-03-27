package com.example.navigation3maintab.maintab

import androidx.compose.runtime.Composable

@Composable
fun MainTabScreen(
    currentTab: MainTab,
    // Inject ViewModel via your DI framework if needed
    onTabSelected: (MainTab) -> Unit = {},
    content: @Composable () -> Unit,
) {
    MainTabScaffold(
        currentTab = currentTab,
        onTabSelected = onTabSelected,
    ) {
        content()
    }
}
