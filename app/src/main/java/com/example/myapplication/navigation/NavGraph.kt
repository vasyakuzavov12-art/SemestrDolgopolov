package com.example.myapplication.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.GameListScreen
import com.example.myapplication.ui.screens.GameDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NavGraph(
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // ПОЛНОЕ УДАЛЕНИЕ БАЗЫ ДАННЫХ
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val databasesPath = context.applicationContext.filesDir.parent + "/databases"
                val databasesDir = File(databasesPath)

                if (databasesDir.exists() && databasesDir.isDirectory) {
                    databasesDir.listFiles()?.forEach { file ->
                        if (file.name.contains("database")) {
                            file.delete()
                        }
                    }
                }
                println("✅ База данных очищена")
            } catch (e: Exception) {
                println("❌ Ошибка: ${e.message}")
            }
        }
    }

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