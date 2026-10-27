package com.example.ui.screen.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.Sender
import com.example.data.model.TaskPlan
import com.example.data.model.UserProfile
import com.example.data.repository.AgentOrchestrator
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.service.JarvisAccessibilityService
import com.example.speech.JarvisSpeechManager
import com.example.ui.components.ArcReactorHUD
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.TaskProgressFloatingOverlay
import com.example.ui.components.ThinkingStepsCard
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisGlassBorder
import com.example.ui.theme.JarvisGlassCard
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRepository: ChatRepository,
    preferencesRepository: UserPreferencesRepository,
    agentOrchestrator: AgentOrchestrator,
    speechManager: JarvisSpeechManager,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messagesFlow = remember {
        chatRepository.observeChats().catch { emit(emptyList()) }
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    val profileFlow = remember {
        preferencesRepository.observeUserProfile().catch { emit(null) }
    }
    val profile by profileFlow.collectAsState(initial = null)

    val currentPlan by agentOrchestrator.currentExecutingPlan.collectAsState()
    val currentStepText by agentOrchestrator.currentStepText.collectAsState()
    val isListening by speechManager.isListening.collectAsState()
    val isSpeaking by speechManager.isSpeaking.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var statusBanner by remember { mutableStateOf("Jarvis standby • Ready for commands") }
    var pendingConfirmationPlan by remember { mutableStateOf<TaskPlan?>(null) }
    var confirmationDeferred by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    var isAccessibilityEnabled by remember {
        mutableStateOf(JarvisAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceCapture(speechManager, onRecognized = { command ->
                inputText = command
                executeCommand(command, chatRepository, agentOrchestrator, messages, profile, speechManager, scope,
                    onRequestConfirmation = { plan ->
                        val deferred = CompletableDeferred<Boolean>()
                        confirmationDeferred = deferred
                        pendingConfirmationPlan = plan
                        deferred.await()
                    },
                    onStatusUpdate = { statusBanner = it }
                )
            })
        }
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size, currentPlan) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        isAccessibilityEnabled = JarvisAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ArcReactorHUD(
                            size = 36.dp,
                            isListening = isListening,
                            isThinking = currentPlan != null,
                            isSpeaking = isSpeaking
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "JARVIS",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = JarvisCyan
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(JarvisCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "3.8 Flash",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = JarvisCyan
                                    )
                                }
                            }
                            Text(
                                text = if (isAccessibilityEnabled) "AUTOMATION ACTIVE" else "TAP TO ENABLE ACCESSIBILITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isAccessibilityEnabled) JarvisSuccess else JarvisWarning,
                                modifier = Modifier.clickable {
                                    if (!isAccessibilityEnabled) {
                                        JarvisAccessibilityService.openAccessibilitySettings(context)
                                    }
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = JarvisCyan
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkBg)
            )
        },
        containerColor = JarvisDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Status Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JarvisDarkSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isListening -> Color(0xFFFF5252)
                                            currentPlan != null -> JarvisCyan
                                            isSpeaking -> JarvisSuccess
                                            else -> JarvisCyan.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = statusBanner,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Chat Messages View
                if (messages.isEmpty() && currentPlan == null) {
                    EmptyChatGreeting(
                        onSelectPrompt = { prompt ->
                            inputText = prompt
                            executeCommand(prompt, chatRepository, agentOrchestrator, messages, profile, speechManager, scope,
                                onRequestConfirmation = { plan ->
                                    val deferred = CompletableDeferred<Boolean>()
                                    confirmationDeferred = deferred
                                    pendingConfirmationPlan = plan
                                    deferred.await()
                                },
                                onStatusUpdate = { statusBanner = it }
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageBubble(msg = msg)
                        }

                        // Live executing plan
                        item {
                            AnimatedVisibility(visible = currentPlan != null) {
                                currentPlan?.let { plan ->
                                    ThinkingStepsCard(plan = plan)
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Chips Carousel
                QuickCommandChips(
                    onSelect = { prompt ->
                        inputText = prompt
                        executeCommand(prompt, chatRepository, agentOrchestrator, messages, profile, speechManager, scope,
                            onRequestConfirmation = { plan ->
                                val deferred = CompletableDeferred<Boolean>()
                                confirmationDeferred = deferred
                                pendingConfirmationPlan = plan
                                deferred.await()
                            },
                            onStatusUpdate = { statusBanner = it }
                        )
                    }
                )

                // Bottom Input Area
                ChatInputBar(
                    inputText = inputText,
                    isListening = isListening,
                    isBusy = currentPlan != null,
                    onTextChanged = { inputText = it },
                    onSend = {
                        val cmd = inputText.trim()
                        if (cmd.isNotBlank()) {
                            inputText = ""
                            executeCommand(cmd, chatRepository, agentOrchestrator, messages, profile, speechManager, scope,
                                onRequestConfirmation = { plan ->
                                    val deferred = CompletableDeferred<Boolean>()
                                    confirmationDeferred = deferred
                                    pendingConfirmationPlan = plan
                                    deferred.await()
                                },
                                onStatusUpdate = { statusBanner = it }
                            )
                        }
                    },
                    onMicClick = {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            if (isListening) {
                                speechManager.stopListening()
                            } else {
                                startVoiceCapture(speechManager, onRecognized = { command ->
                                    inputText = command
                                    executeCommand(command, chatRepository, agentOrchestrator, messages, profile, speechManager, scope,
                                        onRequestConfirmation = { plan ->
                                            val deferred = CompletableDeferred<Boolean>()
                                            confirmationDeferred = deferred
                                            pendingConfirmationPlan = plan
                                            deferred.await()
                                        },
                                        onStatusUpdate = { statusBanner = it }
                                    )
                                })
                            }
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            // Floating progress overlay (when running actions)
            TaskProgressFloatingOverlay(
                currentStepText = currentStepText,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Sensitive action confirmation dialog
            pendingConfirmationPlan?.let { plan ->
                ConfirmationDialog(
                    plan = plan,
                    onConfirm = {
                        confirmationDeferred?.complete(true)
                        pendingConfirmationPlan = null
                    },
                    onDismiss = {
                        confirmationDeferred?.complete(false)
                        pendingConfirmationPlan = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ArcReactorHUD(
            size = 110.dp,
            isThinking = false
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "JARVIS IS ONLINE",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            ),
            color = JarvisCyan
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Ready to control your phone. Try a command below or tap the microphone:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        listOf(
            "Open Instagram and search Powered Tech",
            "Open YouTube and play lofi music",
            "Call Mom",
            "Open Settings and check Wi-Fi",
            "Search Google for latest tech news"
        ).forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisGlassCard)
                    .border(1.dp, JarvisGlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectPrompt(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessage
) {
    val isUser = msg.senderEnum == Sender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) JarvisBlue else JarvisGlassCard)
                .border(
                    1.dp,
                    if (isUser) JarvisCyan.copy(alpha = 0.5f) else JarvisGlassBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                if (!isUser) {
                    Text(
                        text = "JARVIS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = JarvisCyan
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickCommandChips(
    onSelect: (String) -> Unit
) {
    val suggestions = listOf(
        "Open Instagram",
        "Open YouTube",
        "Call Mom",
        "Open Settings",
        "Search AI news",
        "Send SMS to John"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisDarkSurface)
                    .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisCyan
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isListening: Boolean,
    isBusy: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisDarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to voice command…" else "Ask Jarvis anything…",
                        color = if (isListening) Color(0xFFFF5252) else TextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisGlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = JarvisDarkBg,
                    unfocusedContainerColor = JarvisDarkBg
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(Modifier.width(8.dp))

            // Mic Button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFFFF5252) else JarvisGlassCard)
                    .border(
                        1.dp,
                        if (isListening) Color(0xFFFF5252) else JarvisGlassBorder,
                        CircleShape
                    )
                    .testTag("voice_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = if (isListening) Color.White else JarvisCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            // Send Button
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isBusy,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isBusy) JarvisCyan else JarvisGlassCard)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isBusy) Color.Black else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun startVoiceCapture(
    speechManager: JarvisSpeechManager,
    onRecognized: (String) -> Unit
) {
    speechManager.startListening(
        onResult = { text ->
            onRecognized(text)
        },
        onError = { /* handled in manager */ }
    )
}

private fun executeCommand(
    command: String,
    chatRepository: ChatRepository,
    agentOrchestrator: AgentOrchestrator,
    history: List<ChatMessage>,
    profile: UserProfile?,
    speechManager: JarvisSpeechManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestConfirmation: suspend (TaskPlan) -> Boolean,
    onStatusUpdate: (String) -> Unit
) {
    scope.launch {
        // Append user message immediately
        chatRepository.appendMessage(
            ChatMessage(
                text = command,
                sender = Sender.USER.name
            )
        )

        agentOrchestrator.planAndExecute(
            userCommand = command,
            recentHistory = history,
            confirmationLevel = profile?.confirmationLevel ?: "MEDIUM",
            voiceOutput = profile?.voiceOutputEnabled ?: true,
            onRequestConfirmation = onRequestConfirmation,
            onStatusUpdate = onStatusUpdate
        )
    }
}
