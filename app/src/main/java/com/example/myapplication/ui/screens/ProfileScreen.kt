package com.example.myapplication.ui.screens

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.database.AppDatabase
import com.example.myapplication.database.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class ProfileUiState(
    val username: String = "",
    val isLoggedIn: Boolean = false,
    val steamId: String? = null,
    val isLinkingSteam: Boolean = false,
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
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        username = ""
                    )
                }
            }
        }
    }

    fun startSteamLinking() {
        _uiState.value = _uiState.value.copy(isLinkingSteam = true, errorMessage = null)
    }

    fun extractSteamId(claimedId: String): String? {
        val pattern = Pattern.compile("https://steamcommunity.com/openid/id/(\\d+)")
        val matcher = pattern.matcher(claimedId)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun saveSteamId(steamId: String) {
        viewModelScope.launch {
            try {
                val user = database.userDao().getCurrentUser().collect { user ->
                    user?.let {
                        val updatedUser = it.copy(steamId = steamId, steamName = "Steam User")
                        database.userDao().updateUser(updatedUser)
                        _uiState.value = _uiState.value.copy(
                            steamId = steamId,
                            isLinkingSteam = false,
                            successMessage = "Steam аккаунт привязан!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLinkingSteam = false,
                    errorMessage = "Ошибка привязки: ${e.message}"
                )
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
    var showWebView by remember { mutableStateOf(false) }

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
                // Гостевой режим
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "👋 Гость",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Войдите или зарегистрируйтесь, чтобы:\n• Добавлять игры в избранное\n• Оставлять комментарии\n• Привязывать Steam аккаунт",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔐 Войти / Зарегистрироваться")
                        }
                    }
                }
            } else {
                // Авторизованный пользователь
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "👤 Имя: ${uiState.username}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        Text(
                            text = if (uiState.steamId != null)
                                "✅ Steam ID: ${uiState.steamId?.take(10)}..."
                            else
                                "❌ Steam не привязан",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.steamId == null) {
                            Button(
                                onClick = { viewModel.startSteamLinking() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔗 Привязать Steam аккаунт")
                            }
                        } else {
                            Text(
                                text = "🔗 Steam аккаунт привязан!",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (uiState.errorMessage != null) {
                            Text(
                                text = "❌ ${uiState.errorMessage}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (uiState.successMessage != null) {
                            Text(
                                text = "✅ ${uiState.successMessage}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logout {
                                    onLogout()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("🚪 Выйти из аккаунта")
                        }
                    }
                }
            }
        }
    }

    // WebView для привязки Steam
    if (uiState.isLinkingSteam && showWebView) {
        AlertDialog(
            onDismissRequest = {
                showWebView = false
                viewModel.clearMessages()
            },
            title = { Text("Привязка Steam") },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {
                                    if (url != null && url.startsWith("https://steamcommunity.com/openid/login")) {
                                        return false
                                    }
                                    if (url != null && url.contains("steamcommunity.com/openid/id/")) {
                                        val steamId = viewModel.extractSteamId(url)
                                        if (steamId != null) {
                                            viewModel.saveSteamId(steamId)
                                            showWebView = false
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl("https://steamcommunity.com/openid/login?openid.ns=http://specs.openid.net/auth/2.0&openid.mode=checkid_setup&openid.return_to=http://localhost/steam&openid.realm=http://localhost")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWebView = false
                    viewModel.clearMessages()
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    LaunchedEffect(uiState.isLinkingSteam) {
        if (uiState.isLinkingSteam) {
            showWebView = true
        }
    }
}