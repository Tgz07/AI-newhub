package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val articleId: String)

@Serializable
data object SavedRoute

@Serializable
data object SearchRoute

@Serializable
data object SettingsRoute
