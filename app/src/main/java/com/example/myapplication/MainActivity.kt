package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val database = AppDatabase.getInstance(this)
                    var isLoggedIn by remember { mutableStateOf(false) }

                    // Проверяем статус входа
                    LaunchedEffect(Unit) {
                        database.userDao().getCurrentUser().collect { user ->
                            isLoggedIn = user != null
                        }
                    }

                    NavHost(navController = navController, startDestination = "game_list") {
                        composable("game_list") {
                            GameListScreen(
                                onGameClick = { gameId ->
                                    navController.navigate("game_detail/$gameId")
                                },
                                onProfileClick = {
                                    navController.navigate("profile")
                                }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onLoginClick = {
                                    navController.navigate("login")
                                },
                                onLogout = {
                                    navController.popBackStack()
                                    isLoggedIn = false
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.popBackStack()
                                    isLoggedIn = true
                                }
                            )
                        }

                        composable("game_detail/{gameId}") { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId")?.toIntOrNull() ?: 0
                            GameDetailScreen(
                                gameId = gameId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}