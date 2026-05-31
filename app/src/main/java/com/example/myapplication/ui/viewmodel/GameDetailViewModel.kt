package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.GameApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class GameDetailUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val gameDescription: String = "",
    val gameImage: String? = null,
    val developers: String = "",
    val publishers: String = "",
    val releaseDate: String = "",
    val errorMessage: String? = null
)

class GameDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val api: GameApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://store.steampowered.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameApi::class.java)
    }

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val gameId: Int = savedStateHandle["gameId"] ?: -1

    init {
        loadGameDetails()
    }

    private fun loadGameDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val response = api.getGameDetails(gameId)
                val gameData = response[gameId.toString()]?.data

                if (gameData != null) {
                    _uiState.value = GameDetailUiState(
                        isLoading = false,
                        gameName = gameData.displayName,
                        gameDescription = gameData.displayDescription,
                        gameImage = gameData.displayImage,
                        developers = gameData.displayDevelopers,
                        publishers = gameData.displayPublishers,
                        releaseDate = gameData.displayReleaseDate,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = GameDetailUiState(
                        isLoading = false,
                        errorMessage = "Игра не найдена"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = GameDetailUiState(
                    isLoading = false,
                    errorMessage = "Ошибка: ${e.message}"
                )
            }
        }
    }
}