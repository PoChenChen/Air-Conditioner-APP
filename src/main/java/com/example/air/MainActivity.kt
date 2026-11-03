package com.example.air

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.air.data.AppDatabase
import com.example.air.data.ModelRepository
import com.example.air.ui.ModelViewModel
import com.example.air.ui.screens.*
import com.example.air.ui.theme.AirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Create ViewModel with repository
                    val database = AppDatabase.getDatabase(this)
                    val repository = ModelRepository(database.modelDao())
                    val viewModel: ModelViewModel = viewModel {
                        ModelViewModel(repository)
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        
                        composable("camera") {
                            CameraScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        composable("direct_placement") {
                            DirectPlacementScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        composable("model_placement") {
                            ModelPlacementScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        composable("model_list") {
                            ModelListScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        composable("add_model_image") {
                            AddModelImageScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        composable(
                            route = "model_detail/{modelId}",
                            arguments = listOf(
                                navArgument("modelId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val modelId = backStackEntry.arguments?.getLong("modelId") ?: 0L
                            ModelDetailScreen(
                                navController = navController,
                                viewModel = viewModel,
                                modelId = modelId
                            )
                        }
                    }
                }
            }
        }
    }
}