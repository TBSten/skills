package com.example.navigation3maintab.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.example.navigation3maintab.maintab.MainTab
import com.example.navigation3maintab.maintab.MainTabSceneStrategy

@Composable
fun AppNavigation(
    appNavigator: AppNavigator,
) {
    NavDisplay(
        backStack = appNavigator.backstack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategy = remember {
            // MainTabSceneStrategy must be first in the chain
            MainTabSceneStrategy<Screen>()
                .then(DialogSceneStrategy())
                .then(SinglePaneSceneStrategy())
        },
        entryProvider = entryProvider {
            // Tab screens — metadata tells MainTabSceneStrategy to wrap with NavigationBar
            entry<Home>(
                metadata = MainTabSceneStrategy.mainTab(MainTab.Home),
            ) {
                // HomeScreen()
            }

            entry<ChatList>(
                metadata = MainTabSceneStrategy.mainTab(MainTab.ChatList),
            ) {
                // ChatListScreen()
            }

            entry<MyPage>(
                metadata = MainTabSceneStrategy.mainTab(MainTab.MyPage),
            ) {
                // MyPageScreen()
            }

            // Non-tab screens — no metadata, falls through to SinglePaneSceneStrategy
            entry<Settings> { screen ->
                // SettingsScreen()
            }
        },
    )
}
