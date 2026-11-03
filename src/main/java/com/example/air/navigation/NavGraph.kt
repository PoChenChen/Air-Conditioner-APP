package com.example.air.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object DirectPlacement : Screen("direct_placement")
    object ModelPlacement : Screen("model_placement")
    object ModelList : Screen("model_list")
    object ModelDetail : Screen("model_detail/{modelId}") {
        fun createRoute(modelId: Long) = "model_detail/$modelId"
    }
    object AddModelImage : Screen("add_model_image")
} 