package com.example.ui.screen.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.service.JarvisAccessibilityService
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisError
import com.example.ui.theme.JarvisGlassBorder
import com.example.ui.theme.JarvisGlassCard
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: UserPreferencesRepository,
    chatRepository: ChatRepository,
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profileFlow = remember {
        preferencesRepository.observeUserProfile().catch { emit(null) }
    }
    val profile by profileFlow.collectAsState(initial = null)

    var temperature by remember(profile) {
        mutableFloatStateOf(profile?.modelTemperature?.toFloat() ?: 0.2f)
    }
    var voiceSpeed by remember(profile) {
        mutableFloatStateOf(profile?.voiceSpeed?.toFloat() ?: 1.0f)
    }
    var confirmationLevel by remember(profile) {
        mutableStateOf(profile?.confirmationLevel ?: "MEDIUM")
    }
    var wakeWordEnabled by remember(profile) {
        mutableStateOf(profile?.wakeWordEnabled ?: false)
    }
    var pauseControl by remember(profile) {
        mutableStateOf(profile?.pauseControl ?: false)
    }
    var voiceOutputEnabled by remember(profile) {
        mutableStateOf(profile?.voiceOutputEnabled ?: true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & SYSTEM",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = JarvisCyan
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisDarkBg)
            )
        },
        containerColor = JarvisDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: AI & Reasoning
            SettingsSectionHeader(icon = Icons.Default.Psychology, title = "AI BRAIN & MODEL")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisGlassCard)
                    .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Temperature (Creativity)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                        Text(
                            text = String.format("%.2f", temperature),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = JarvisCyan
                        )
                    }
                    Text(
                        text = "Lower values produce more deterministic, precise phone actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = it
                            savePreferences(preferencesRepository, scope, profile, temp = it)
                        },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisDarkSurface
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Safety Confirmation Level",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                    Text(
                        text = "Low: only irreversible actions. Medium: calls & sms. High: always confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { level ->
                            FilterChip(
                                selected = confirmationLevel == level,
                                onClick = {
                                    confirmationLevel = level
                                    savePreferences(preferencesRepository, scope, profile, confirmLevel = level)
                                },
                                label = { Text(level) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // Section 2: Speech & Voice
            SettingsSectionHeader(icon = Icons.Default.VolumeUp, title = "VOICE & SPEECH")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisGlassCard)
                    .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Responses (TTS)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                            Text(
                                text = "Jarvis speaks results and execution summaries aloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = voiceOutputEnabled,
                            onCheckedChange = {
                                voiceOutputEnabled = it
                                savePreferences(preferencesRepository, scope, profile, voiceOut = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = JarvisDeepBlue
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hey Jarvis Wake Word",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                            Text(
                                text = "Listen for activation keyword in background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                                savePreferences(preferencesRepository, scope, profile, wakeWord = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = JarvisDeepBlue
                            )
                        )
                    }
                }
            }

            // Section 3: System Automation Permissions
            SettingsSectionHeader(icon = Icons.Default.Security, title = "PHONE CONTROL & PERMISSIONS")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisGlassCard)
                    .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pause Phone Automation",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                            Text(
                                text = "Only answer questions without performing taps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = pauseControl,
                            onCheckedChange = {
                                pauseControl = it
                                savePreferences(preferencesRepository, scope, profile, pause = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyan,
                                checkedTrackColor = JarvisDeepBlue
                            )
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { JarvisAccessibilityService.openAccessibilitySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Accessibility, contentDescription = null, tint = JarvisCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Configure Accessibility Service", color = TextPrimary)
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { JarvisAccessibilityService.openOverlaySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = JarvisCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Floating Overlay Permission", color = TextPrimary)
                    }
                }
            }

            // Section 4: Data & Account
            Button(
                onClick = {
                    scope.launch {
                        try {
                            chatRepository.clearHistory()
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisDarkSurface,
                    contentColor = JarvisError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear Conversation History")
            }

            Button(
                onClick = {
                    val credentialManager = CredentialManager.create(context)
                    Firebase.auth.signOut()
                    scope.launch {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (_: Exception) {}
                        onSignedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisDarkSurface,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out from Jarvis")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Jarvis AI v1.0.0 • Google Gemini 3.8 Flash • Firebase Backend",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            ),
            color = JarvisCyan
        )
    }
}

private fun savePreferences(
    repository: UserPreferencesRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    existing: UserProfile?,
    temp: Float? = null,
    voiceSpd: Float? = null,
    confirmLevel: String? = null,
    wakeWord: Boolean? = null,
    pause: Boolean? = null,
    voiceOut: Boolean? = null
) {
    val current = existing ?: UserProfile()
    val updated = current.copy(
        modelTemperature = temp?.toDouble() ?: current.modelTemperature,
        voiceSpeed = voiceSpd?.toDouble() ?: current.voiceSpeed,
        confirmationLevel = confirmLevel ?: current.confirmationLevel,
        wakeWordEnabled = wakeWord ?: current.wakeWordEnabled,
        pauseControl = pause ?: current.pauseControl,
        voiceOutputEnabled = voiceOut ?: current.voiceOutputEnabled
    )
    scope.launch {
        try {
            repository.saveUserProfile(updated)
        } catch (_: Exception) {}
    }
}
