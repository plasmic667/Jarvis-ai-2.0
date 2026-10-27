package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.UserProfile
import com.example.data.repository.AgentOrchestrator
import com.example.data.repository.ChatRepository
import com.example.data.repository.JarvisRepository
import com.example.data.repository.TaskHistoryRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.speech.JarvisSpeechManager
import com.example.ui.screen.auth.AuthScreen
import com.example.ui.screen.chat.ChatScreen
import com.example.ui.screen.history.HistoryScreen
import com.example.ui.screen.onboarding.OnboardingScreen
import com.example.ui.screen.settings.SettingsScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppDestination {
    CHAT,
    HISTORY,
    SETTINGS,
    ONBOARDING
}

class MainActivity : ComponentActivity() {

    private var speechManager: JarvisSpeechManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val speech = JarvisSpeechManager(this)
        speechManager = speech

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBackground
                ) {
                    JarvisAppRoot(context = this, speechManager = speech)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager?.shutdown()
    }
}

@Composable
fun JarvisAppRoot(context: Context, speechManager: JarvisSpeechManager) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = JarvisRepository(context)
                        repo.saveUserProfile(
                            UserProfile(
                                userId = currentUser!!.uid,
                                displayName = currentUser!!.displayName ?: "Jarvis User",
                                email = currentUser!!.email ?: ""
                            )
                        )
                    } catch (_: Exception) {
                        // Handled gracefully
                    }
                }
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        JarvisMainNavigation(
            context = context,
            speechManager = speechManager,
            currentUserId = currentUser!!.uid
        )
    }
}

@Composable
fun JarvisMainNavigation(
    context: Context,
    speechManager: JarvisSpeechManager,
    currentUserId: String
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }

    val chatRepository = remember(currentUserId) { ChatRepository(context) }
    val preferencesRepository = remember(currentUserId) { UserPreferencesRepository(context) }
    val historyRepository = remember(currentUserId) { TaskHistoryRepository(context) }
    val agentOrchestrator = remember(currentUserId) {
        AgentOrchestrator(
            context = context,
            chatRepository = chatRepository,
            historyRepository = historyRepository,
            speechManager = speechManager
        )
    }

    when (currentDestination) {
        AppDestination.CHAT -> {
            ChatScreen(
                chatRepository = chatRepository,
                preferencesRepository = preferencesRepository,
                agentOrchestrator = agentOrchestrator,
                speechManager = speechManager,
                onNavigateToHistory = { currentDestination = AppDestination.HISTORY },
                onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
            )
        }
        AppDestination.HISTORY -> {
            HistoryScreen(
                taskHistoryRepository = historyRepository,
                onBack = { currentDestination = AppDestination.CHAT },
                onRepeatTask = { command ->
                    currentDestination = AppDestination.CHAT
                }
            )
        }
        AppDestination.SETTINGS -> {
            SettingsScreen(
                preferencesRepository = preferencesRepository,
                chatRepository = chatRepository,
                onBack = { currentDestination = AppDestination.CHAT },
                onSignedOut = {
                    currentDestination = AppDestination.CHAT
                }
            )
        }
        AppDestination.ONBOARDING -> {
            OnboardingScreen(
                onFinish = { currentDestination = AppDestination.CHAT }
            )
        }
    }
}
