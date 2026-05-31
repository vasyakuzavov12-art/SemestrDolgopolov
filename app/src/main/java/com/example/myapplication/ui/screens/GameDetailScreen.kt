package com.example.myapplication.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.database.CommentEntity

data class GameDetailUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val gameDescription: String = "",
    val gameImage: String? = null,
    val developers: String = "",
    val publishers: String = "",
    val releaseDate: String = "",
    val genres: String = "",
    val website: String = "",
    val steamUrl: String = "",
    val comments: List<CommentEntity> = emptyList(),
    val newCommentText: String = "",
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class GameDetailViewModel(private val gameId: Int, private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val api = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GameApi::class.java)

    private val database = AppDatabase.getInstance(context)

    init {
        loadGameDetails()
        loadComments()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            database.userDao().getCurrentUser().collect { user ->
                _uiState.value = _uiState.value.copy(isLoggedIn = user != null)
            }
        }
    }

    private fun loadGameDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = api.getGameDetails(gameId, language = "russian")
                val gameData = response[gameId.toString()]?.data

                if (gameData != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        gameName = gameData.displayName,
                        gameDescription = gameData.displayDescription,
                        gameImage = gameData.displayImage,
                        developers = gameData.displayDevelopers,
                        publishers = gameData.displayPublishers,
                        releaseDate = gameData.displayReleaseDate,
                        genres = gameData.displayGenres,
                        website = gameData.website ?: "",
                        steamUrl = "https://store.steampowered.com/app/${gameId}",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        steamUrl = "https://store.steampowered.com/app/${gameId}",
                        errorMessage = "Игра не найдена"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    steamUrl = "https://store.steampowered.com/app/${gameId}",
                    errorMessage = "Ошибка: ${e.message}"
                )
            }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            database.commentDao().getCommentsForGame(gameId).collect { comments ->
                _uiState.value = _uiState.value.copy(comments = comments)
            }
        }
    }

    fun updateNewCommentText(text: String) {
        _uiState.value = _uiState.value.copy(newCommentText = text)
    }

    fun addComment() {
        viewModelScope.launch {
            val text = _uiState.value.newCommentText
            if (text.isBlank()) return@launch

            if (!_uiState.value.isLoggedIn) {
                _uiState.value = _uiState.value.copy(errorMessage = "Войдите в аккаунт, чтобы оставить комментарий")
                return@launch
            }

            val currentUser = database.userDao().getCurrentUser().first()

            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Войдите в аккаунт, чтобы оставить комментарий")
                return@launch
            }

            val comment = CommentEntity(
                gameId = gameId,
                userId = currentUser.id,
                username = currentUser.username,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            database.commentDao().addComment(comment)
            _uiState.value = _uiState.value.copy(newCommentText = "")
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class GameDetailViewModelFactory(private val gameId: Int, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameDetailViewModel(gameId, context) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(gameId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val factory = remember { GameDetailViewModelFactory(gameId, context) }
    val viewModel: GameDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали игры") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.errorMessage != null && uiState.gameName.isEmpty() ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌ ${uiState.errorMessage}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Назад") }
                    }
                else ->
                    // Используем LazyColumn вместо Column + verticalScroll
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Картинка
                        item {
                            if (uiState.gameImage != null) {
                                AsyncImage(
                                    model = uiState.gameImage,
                                    contentDescription = uiState.gameName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }

                        // Название
                        item {
                            Text(uiState.gameName, style = MaterialTheme.typography.headlineMedium)
                        }

                        // Жанры
                        if (uiState.genres.isNotEmpty() && uiState.genres != "Не указан") {
                            item {
                                Text("🎮 Жанр: ${uiState.genres}", style = MaterialTheme.typography.titleSmall)
                            }
                        }

                        // Описание
                        item {
                            Text("📝 Описание:", style = MaterialTheme.typography.titleMedium)
                            Text(uiState.gameDescription, style = MaterialTheme.typography.bodyMedium)
                        }

                        // Детали
                        item {
                            Text("👨‍💻 Разработчик: ${uiState.developers}", style = MaterialTheme.typography.bodySmall)
                            Text("🏢 Издатель: ${uiState.publishers}", style = MaterialTheme.typography.bodySmall)
                            Text("📅 Дата релиза: ${uiState.releaseDate}", style = MaterialTheme.typography.bodySmall)
                        }

                        // Кнопка Steam
                        item {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.steamUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🌐 Открыть в Steam Store")
                            }
                        }

                        // === СЕКЦИЯ КОММЕНТАРИЕВ ===
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💬 Комментарии (${uiState.comments.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        // Поле для нового комментария
                        item {
                            OutlinedTextField(
                                value = uiState.newCommentText,
                                onValueChange = { viewModel.updateNewCommentText(it) },
                                label = { Text(if (uiState.isLoggedIn) "Ваш комментарий..." else "Войдите в аккаунт, чтобы оставить комментарий") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                enabled = uiState.isLoggedIn
                            )
                        }

                        item {
                            Button(
                                onClick = { viewModel.addComment() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.isLoggedIn
                            ) {
                                Text("✏️ Отправить комментарий")
                            }
                        }

                        // Список комментариев
                        if (uiState.comments.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Text(
                                        text = "Пока нет комментариев. Будьте первым!",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(uiState.comments) { comment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "👤 ${comment.username}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                                    .format(java.util.Date(comment.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodyMedium
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