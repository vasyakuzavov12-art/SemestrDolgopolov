package com.example.myapplication.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
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
import com.example.myapplication.database.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val username: String = "",
    val isLoggedIn: Boolean = false,
    val steamId: String? = null,
    val showSteamInputDialog: Boolean = false,
    val tempSteamId: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getInstance(context)

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            database.userDao().getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        username = user.username,
                        isLoggedIn = true,
                        steamId = user.steamId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoggedIn = false)
                }
            }
        }
    }

    fun showSteamInputDialog() {
        _uiState.value = _uiState.value.copy(
            showSteamInputDialog = true,
            tempSteamId = _uiState.value.steamId ?: "",
            errorMessage = null
        )
    }

    fun updateTempSteamId(value: String) {
        _uiState.value = _uiState.value.copy(tempSteamId = value)
    }

    fun saveSteamId() {
        val steamId = _uiState.value.tempSteamId.trim()

        if (steamId.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Введите Steam ID")
            return
        }

        if (!steamId.matches(Regex("^\\d+$"))) {
            _uiState.value = _uiState.value.copy(errorMessage = "Steam ID должен содержать только цифры")
            return
        }

        viewModelScope.launch {
            try {
                val user = database.userDao().getCurrentUser().first()
                if (user != null) {
                    val updatedUser = user.copy(steamId = steamId, steamName = "Steam User")
                    database.userDao().updateUser(updatedUser)
                    _uiState.value = _uiState.value.copy(
                        steamId = steamId,
                        showSteamInputDialog = false,
                        tempSteamId = "",
                        successMessage = "Steam ID сохранён!"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            database.userDao().logoutAll()
            _uiState.value = _uiState.value.copy(isLoggedIn = false, username = "", steamId = null)
            onLogout()
        }
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(showSteamInputDialog = false, tempSteamId = "", errorMessage = null)
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(context) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val factory = remember { ProfileViewModelFactory(context) }
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        kotlinx.coroutines.delay(3000)
        viewModel.clearMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("👋 Гость", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Войдите или зарегистрируйтесь, чтобы:\n• Добавлять игры в избранное\n• Оставлять комментарии\n• Привязывать Steam аккаунт",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                            Text("🔐 Войти / Зарегистрироваться")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("👤 Имя: ${uiState.username}", style = MaterialTheme.typography.titleMedium)
                        Divider()

                        Text(
                            text = if (uiState.steamId != null)
                                "✅ Steam ID: ${uiState.steamId}"
                            else
                                "❌ Steam не привязан",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = { viewModel.showSteamInputDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.steamId == null) "🔗 Привязать Steam ID" else "✏️ Изменить Steam ID")
                        }

                        if (uiState.errorMessage != null) {
                            Text("❌ ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                        }
                        if (uiState.successMessage != null) {
                            Text("✅ ${uiState.successMessage}", color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.logout(onLogout) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("🚪 Выйти из аккаунта")
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSteamInputDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDialog() },
            title = { Text("🔗 Привязка Steam ID") },
            text = {
                Column {
                    Text("Введите ваш Steam ID (число):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.tempSteamId,
                        onValueChange = { viewModel.updateTempSteamId(it) },
                        label = { Text("Steam ID") },
                        placeholder = { Text("Например: 76561197960435530") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Как найти Steam ID: Откройте профиль Steam в браузере → скопируйте URL → число в конце",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveSteamId() }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDialog() }) {
                    Text("Отмена")
                }
            }
        )
    }
}