package com.example.navigation3maintab.navigation

import com.example.navigation3maintab.maintab.MainTab
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen

@Serializable
data object Home : Screen

@Serializable
data object ChatList : Screen

@Serializable
data object MyPage : Screen

@Serializable
data object Settings : Screen

/**
 * [MainTab] と [Screen] の対応関係をここに集約する。
 * タブ追加時は [mainTabOrNull] と [MainTab.screen] を同時に更新すること。
 */

/** [Screen] に対応する [MainTab] を返す。タブ画面でなければ null。 */
val Screen.mainTabOrNull: MainTab?
    get() = when (this) {
        Home -> MainTab.Home
        ChatList -> MainTab.ChatList
        MyPage -> MainTab.MyPage
        else -> null
    }

/** [MainTab] に対応する [Screen] を返す。 */
val MainTab.screen: Screen
    get() = when (this) {
        MainTab.Home -> Home
        MainTab.ChatList -> ChatList
        MainTab.MyPage -> MyPage
    }
