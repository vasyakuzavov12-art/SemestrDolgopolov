package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.GameListScreen
import com.example.myapplication.ui.screens.GameDetailScreen

@Composable
fun NavGraph(
    onProfileClick: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "game_list"
    ) {
        composable("game_list") {
            GameListScreen(
                onGameClick = { gameId ->
                    navController.navigate("game_detail/$gameId")
                },
                onProfileClick = onProfileClick
            )
        }

        composable(
            "game_detail/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: 0
            GameDetailScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}