package com.example.myapplication.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.api.GameApi
import com.example.myapplication.api.SteamGameItem
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.database.FavoriteGameEntity

data class GameUi(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val price: String,
    val steamUrl: String,
    val isFavorite: Boolean = false
)

data class GameListUiState(
    val isLoading: Boolean = false,
    val games: List<GameUi> = emptyList(),
    val allGames: List<SteamGameItem> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isOfflineMode: Boolean = false,
    val showFavoritesOnly: Boolean = false,
    val searchPerformed: Boolean = false,
    val currentUserId: Long = 0
)

class GameListViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(GameListUiState())
    val uiState: StateFlow<GameListUiState> = _uiState.asStateFlow()

    private val api = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GameApi::class.java)

    private val database = AppDatabase.getInstance(context)
    private val favoriteIds = mutableSetOf<Int>()
    private var currentUserId: Long = 0

    init {
        loadCurrentUser()
        loadFavorites()
        searchGames("game")
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            database.userDao().getCurrentUser().collect { user ->
                if (user != null) {
                    currentUserId = user.id
                    _uiState.value = _uiState.value.copy(currentUserId = user.id)
                    loadFavorites()
                } else {
                    currentUserId = 0L
                    _uiState.value = _uiState.value.copy(currentUserId = 0L)
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            if (currentUserId != 0L) {
                database.favoriteGameDao().getAllFavorites().collect { favorites ->
                    favoriteIds.clear()
                    favoriteIds.addAll(favorites.map { it.id })
                    updateGamesWithFavorites()
                }
            }
        }
    }

    private fun updateGamesWithFavorites() {
        val currentGames = _uiState.value.games
        _uiState.value = _uiState.value.copy(
            games = currentGames.map { game ->
                game.copy(isFavorite = favoriteIds.contains(game.id))
            }
        )
    }

    fun toggleFavorite(game: GameUi) {
        viewModelScope.launch {
            if (currentUserId == 0L) {
                _uiState.value = _uiState.value.copy(errorMessage = "Войдите в аккаунт, чтобы добавлять в избранное")
                return@launch
            }

            if (favoriteIds.contains(game.id)) {
                val entity = FavoriteGameEntity(game.id, currentUserId, game.name, game.imageUrl, game.price)
                database.favoriteGameDao().removeFromFavorites(entity)
                favoriteIds.remove(game.id)
            } else {
                val entity = FavoriteGameEntity(game.id, currentUserId, game.name, game.imageUrl, game.price)
                database.favoriteGameDao().addToFavorites(entity)
                favoriteIds.add(game.id)
            }
            updateGamesWithFavorites()
        }
    }

    fun toggleShowFavoritesOnly() {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = !_uiState.value.showFavoritesOnly)
        if (_uiState.value.showFavoritesOnly) {
            loadFavoritesOnly()
        } else if (_uiState.value.searchPerformed) {
            searchGames(_uiState.value.searchQuery)
        }
    }

    private fun loadFavoritesOnly() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val favorites = database.favoriteGameDao().getAllFavorites().first()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                games = favorites.map {
                    GameUi(
                        id = it.id,
                        name = it.name,
                        imageUrl = it.imageUrl,
                        price = it.price,
                        steamUrl = "https://store.steampowered.com/app/${it.id}",
                        isFavorite = true
                    )
                },
                searchPerformed = true
            )
        }
    }

    fun searchGames(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                searchQuery = query,
                searchPerformed = true
            )

            try {
                val response = api.searchGames(term = query, count = 50)

                val games = response.items.map { item ->
                    GameUi(
                        id = item.id,
                        name = item.displayName,
                        imageUrl = item.displayImage,
                        price = item.formattedPrice,
                        steamUrl = item.steamUrl,
                        isFavorite = favoriteIds.contains(item.id)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    games = games,
                    allGames = response.items,
                    errorMessage = if (games.isEmpty()) "Игры не найдены" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        if (_uiState.value.showFavoritesOnly) {
            loadFavoritesOnly()
        } else if (_uiState.value.searchQuery.isNotBlank()) {
            searchGames(_uiState.value.searchQuery)
        }
    }
}

class GameListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameListViewModel(context) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(onGameClick: (Int) -> Unit, onProfileClick: () -> Unit) {
    val context = LocalContext.current
    val factory = remember { GameListViewModelFactory(context) }
    val viewModel: GameListViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.isOfflineMode) {
        if (uiState.isOfflineMode && uiState.games.isNotEmpty()) {
            snackbarHostState.showSnackbar("📱 Офлайн-режим: показаны сохраненные игры")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showFavoritesOnly) "Избранное ❤️" else "Steam Games 🎮") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, "Профиль")
                    }
                    IconButton(onClick = { viewModel.toggleShowFavoritesOnly() }) {
                        Icon(
                            if (uiState.showFavoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (uiState.showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Обновить")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("🔍 Введите название игры...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Button(
                        onClick = {
                            if (searchText.isNotBlank()) {
                                viewModel.searchGames(searchText)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Найти")
                    }
                }
            }

            if (uiState.searchPerformed) {
                Text(
                    text = "🔍 Результаты поиска: ${uiState.games.size}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.games.isEmpty() ->
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Поиск игр...")
                        }

                    uiState.errorMessage != null && uiState.games.isEmpty() ->
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌ ${uiState.errorMessage}")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) { Text("Повторить") }
                        }

                    uiState.games.isEmpty() && uiState.showFavoritesOnly ->
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❤️ Нет игр в избранном")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Добавляйте игры через сердечко", style = MaterialTheme.typography.bodySmall)
                        }

                    uiState.games.isEmpty() && uiState.searchPerformed ->
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎮 Ничего не найдено")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Попробуйте другое название", style = MaterialTheme.typography.bodySmall)
                        }

                    else ->
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.games) { game ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onGameClick(game.id) },
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            AsyncImage(
                                                model = game.imageUrl,
                                                contentDescription = game.name,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .padding(end = 12.dp)
                                            )
                                            Column {
                                                Text(
                                                    game.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 2
                                                )
                                                Text(
                                                    "💰 ${game.price}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        IconButton(onClick = { viewModel.toggleFavorite(game) }) {
                                            Icon(
                                                if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = "Избранное",
                                                tint = if (game.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}