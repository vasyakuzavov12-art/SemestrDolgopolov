package com.example.myapplication.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myapplication.api.GameApi
import com.example.myapplication.api.SteamGameItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T, val fromCache: Boolean = false) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

class GameRepository(private val context: Context) {

    private val api: GameApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://store.steampowered.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameApi::class.java)
    }

    fun getGames(): Flow<ApiResult<List<SteamGameItem>>> = flow {
        emit(ApiResult.Loading)

        if (isNetworkAvailable()) {
            try {
                val response = api.searchGames(term = "game")
                emit(ApiResult.Success(response.items, fromCache = false))
            } catch (e: Exception) {
                emit(ApiResult.Error("Ошибка сети: ${e.message}"))
            }
        } else {
            emit(ApiResult.Error("Нет интернета"))
        }
    }

    suspend fun getGameDetails(gameId: Int): ApiResult<com.example.myapplication.api.GameData?> {
        return try {
            val response = api.getGameDetails(gameId)
            val gameData = response[gameId.toString()]?.data
            if (gameData != null) {
                ApiResult.Success(gameData)
            } else {
                ApiResult.Error("Игра не найдена")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Ошибка загрузки")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}