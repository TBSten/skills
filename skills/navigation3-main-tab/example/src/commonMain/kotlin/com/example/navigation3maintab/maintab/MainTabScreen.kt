package com.example.navigation3maintab.maintab

import androidx.compose.runtime.Composable

@Composable
fun MainTabScreen(
    currentTab: MainTab,
    // CUSTOMIZE: 必要なら DI フレームワーク経由で ViewModel を注入する
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
