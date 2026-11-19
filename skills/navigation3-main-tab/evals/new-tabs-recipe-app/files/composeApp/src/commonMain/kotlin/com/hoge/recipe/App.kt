package com.hoge.recipe

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.hoge.recipe.ui.FeedScreen

@Composable
fun App() {
    MaterialTheme {
        // TODO: 下タブ付きのナビゲーションにしたい
        FeedScreen(onRecipeClick = {})
    }
}
