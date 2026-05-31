package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.GameApi
import com.example.myapplication.api.SteamGameItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class GameListUiState(
    val isLoading: Boolean = false,
    val games: List<SteamGameItem> = emptyList(),
    val errorMessage: String? = null
)

class GameListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameListUiState())
    val uiState: StateFlow<GameListUiState> = _uiState.asStateFlow()

    private val api = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GameApi::class.java)

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.searchGames(term = "game")
                _uiState.value = GameListUiState(
                    isLoading = false,
                    games = response.items,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = GameListUiState(
                    isLoading = false,
                    games = emptyList(),
                    errorMessage = e.message
                )
            }
        }
    }

    fun refresh() = loadGames()
}