package com.hoge.recipe.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun FeedScreen(onRecipeClick: (recipeId: String) -> Unit) { Text("Feed") }

@Composable
fun SearchScreen(onRecipeClick: (recipeId: String) -> Unit) { Text("Search") }

@Composable
fun ProfileScreen() { Text("Profile") }

@Composable
fun RecipeDetailScreen(recipeId: String, onBack: () -> Unit) { Text("Recipe $recipeId") }
