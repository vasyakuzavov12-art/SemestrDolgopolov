package com.example.myapplication.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.api.GameApi
import com.example.myapplication.api.Screenshot
import com.example.myapplication.api.Movie
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.database.CommentEntity
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
data class GameDetailUiState(
    val isLoading: Boolean = false,
    val gameName: String = "",
    val gameDescription: String = "",
    val gameImage: String? = null,
    val developers: String = "",
    val publishers: String = "",
    val releaseDate: String = "",
    val genres: String = "",
    val steamUrl: String = "",
    val price: String = "",
    val screenshots: List<Screenshot> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val comments: List<CommentEntity> = emptyList(),
    val newCommentText: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedMediaIndex: Int = -1,
    val selectedMediaType: String = ""
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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = api.getGameDetails(gameId, language = "english")
                val gameData = response[gameId.toString()]?.data

                if (gameData != null && gameData.name != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        gameName = gameData.name ?: "Unknown",
                        gameDescription = gameData.detailed_description?.take(500) ?: gameData.short_description ?: "Нет описания",
                        gameImage = gameData.header_image,
                        developers = gameData.developers?.joinToString(", ") ?: "Не указаны",
                        publishers = gameData.publishers?.joinToString(", ") ?: "Не указаны",
                        releaseDate = gameData.release_date?.date ?: "Не указана",
                        genres = gameData.genres?.joinToString(", ") { it.name ?: "" } ?: "Не указан",
                        steamUrl = "https://store.steampowered.com/app/${gameId}",
                        screenshots = gameData.screenshots ?: emptyList(),
                        movies = gameData.movies ?: emptyList(),
                        price = "Не указана",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        gameName = "Игра ID: $gameId",
                        gameDescription = "Информация временно недоступна",
                        steamUrl = "https://store.steampowered.com/app/${gameId}",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    gameName = "Игра ID: $gameId",
                    gameDescription = "Ошибка загрузки. Нажмите кнопку ниже.",
                    steamUrl = "https://store.steampowered.com/app/${gameId}",
                    errorMessage = null
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
                _uiState.value = _uiState.value.copy(errorMessage = "Войдите в аккаунт")
                delay(2000)
                _uiState.value = _uiState.value.copy(errorMessage = null)
                return@launch
            }

            val currentUser = database.userDao().getCurrentUser().first()

            if (currentUser == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Войдите в аккаунт")
                delay(2000)
                _uiState.value = _uiState.value.copy(errorMessage = null)
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
            _uiState.value = _uiState.value.copy(newCommentText = "", successMessage = "Комментарий добавлен")
            delay(2000)
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }

    fun showMedia(index: Int, type: String) {
        _uiState.value = _uiState.value.copy(selectedMediaIndex = index, selectedMediaType = type)
    }

    fun closeMediaDialog() {
        _uiState.value = _uiState.value.copy(selectedMediaIndex = -1, selectedMediaType = "")
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
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

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it) }
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    item {
                        if (!uiState.gameImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = uiState.gameImage,
                                contentDescription = uiState.gameName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }


                    item {
                        Text(uiState.gameName, style = MaterialTheme.typography.headlineMedium)
                    }


                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💰 ${uiState.price}", style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = { viewModel.openUrl(uiState.steamUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("🌐 Открыть в Steam")
                            }
                        }
                    }


                    if (uiState.genres.isNotEmpty() && uiState.genres != "Не указан") {
                        item {
                            Text("🎮 Жанр: ${uiState.genres}", style = MaterialTheme.typography.titleSmall)
                        }
                    }


                    item {
                        Text("📝 Описание:", style = MaterialTheme.typography.titleMedium)
                        Text(uiState.gameDescription, style = MaterialTheme.typography.bodyMedium)
                    }


                    item {
                        Text("👨‍💻 Разработчик: ${uiState.developers}", style = MaterialTheme.typography.bodySmall)
                        Text("🏢 Издатель: ${uiState.publishers}", style = MaterialTheme.typography.bodySmall)
                        Text("📅 Дата релиза: ${uiState.releaseDate}", style = MaterialTheme.typography.bodySmall)
                    }


                    if (uiState.screenshots.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("📸 Скриншоты:", style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(uiState.screenshots) { screenshot ->
                                    Card(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(84.dp)
                                            .clickable {
                                                viewModel.openUrl(screenshot.path_full)
                                            },
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        AsyncImage(
                                            model = screenshot.path_thumbnail,
                                            contentDescription = "Screenshot",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }


                    if (uiState.movies.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🎬 Трейлеры:", style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(uiState.movies) { movie ->
                                    val videoUrl = movie.mp4?.max ?: movie.webm?.max
                                    if (videoUrl != null) {
                                        Card(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(84.dp)
                                                .clickable {
                                                    viewModel.openUrl(videoUrl)
                                                },
                                            elevation = CardDefaults.cardElevation(4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(
                                                    model = movie.thumbnail,
                                                    contentDescription = movie.name,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(32.dp),
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("💬 Комментарии (${uiState.comments.size})", style = MaterialTheme.typography.titleLarge)
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.newCommentText,
                            onValueChange = { viewModel.updateNewCommentText(it) },
                            label = { Text(if (uiState.isLoggedIn) "Ваш комментарий..." else "Войдите в аккаунт") },
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
                        ) { Text("✏️ Отправить") }
                    }

                    if (uiState.comments.isEmpty()) {
                        item {
                            Text("Пока нет комментариев", modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(uiState.comments) { comment ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("👤 ${comment.username}", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                                .format(java.util.Date(comment.timestamp)),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}