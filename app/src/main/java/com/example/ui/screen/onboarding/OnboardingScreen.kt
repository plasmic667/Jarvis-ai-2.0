package com.example.ui.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
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

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }

    var isMicGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isAccessibilityGranted by remember {
        mutableStateOf(JarvisAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    var isOverlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isMicGranted = granted
    }

    // Refresh permissions when returning to screen
    LaunchedEffect(Unit) {
        isAccessibilityGranted = JarvisAccessibilityService.isAccessibilityServiceEnabled(context)
        isOverlayGranted = Settings.canDrawOverlays(context)
        isMicGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(Modifier.height(24.dp))

            // Slide indicator dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                (0..2).forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (currentSlide == index) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (currentSlide == index) JarvisCyan else JarvisGlassBorder)
                            .clickable { currentSlide = index }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Slides Content
            when (currentSlide) {
                0 -> SlideWelcome()
                1 -> SlideCapabilities()
                2 -> SlidePermissions(
                    context = context,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isMicGranted = isMicGranted,
                    isOverlayGranted = isOverlayGranted,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onOpenAccessibility = { JarvisAccessibilityService.openAccessibilitySettings(context) },
                    onOpenOverlay = { JarvisAccessibilityService.openOverlaySettings(context) }
                )
            }
        }

        // Bottom Navigation Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp)
        ) {
            if (currentSlide < 2) {
                Button(
                    onClick = { currentSlide++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_start_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text("Enter Command Center", fontWeight = FontWeight.Bold)
                }
            }

            if (currentSlide > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { currentSlide-- },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Back", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SlideWelcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(JarvisCyan.copy(alpha = 0.12f))
                .border(1.5.dp, JarvisCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Welcome to Jarvis AI",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Control your phone with natural voice and text commands. Let Jarvis do the tapping, typing, and navigating.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Example Command Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(JarvisGlassCard)
                .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "NATURAL COMMAND EXAMPLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = JarvisCyan
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\"Hey Jarvis, open Instagram, search for Powered Tech, follow them, and comment 'Amazing content!'\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SlideCapabilities() {
    Column {
        Text(
            text = "How Jarvis Operates",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Powered by Google Gemini 3.8 Flash, Jarvis breaks down your instructions into machine-readable phone operations:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        CapabilityItem(
            stepNumber = "1",
            title = "Deep Goal Comprehension",
            description = "Analyzes complex multi-app commands and recent conversational context."
        )

        Spacer(Modifier.height(14.dp))

        CapabilityItem(
            stepNumber = "2",
            title = "Autonomous Step Planning",
            description = "Creates structured steps (open app, tap text, type field, scroll list)."
        )

        Spacer(Modifier.height(14.dp))

        CapabilityItem(
            stepNumber = "3",
            title = "Live Feedback & Self-Correction",
            description = "Shows live 'Thinking…' sequence and automatically adapts if an element shifts."
        )

        Spacer(Modifier.height(14.dp))

        CapabilityItem(
            stepNumber = "4",
            title = "Zero-Trust Safety Confirmation",
            description = "Sensitive actions like calls, SMS, and financial transactions always ask for your explicit confirmation."
        )
    }
}

@Composable
private fun CapabilityItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisDarkSurface)
            .border(1.dp, JarvisGlassBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(JarvisCyan.copy(alpha = 0.15f))
                .border(1.dp, JarvisCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = JarvisCyan
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SlidePermissions(
    context: Context,
    isAccessibilityGranted: Boolean,
    isMicGranted: Boolean,
    isOverlayGranted: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit
) {
    Column {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "To automate phone operations and listen to your voice, please enable these essential Android capabilities:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(18.dp))

        // 1. Accessibility Service
        PermissionCard(
            title = "Accessibility Automation",
            description = "Allows Jarvis to open apps and tap buttons on your behalf.",
            isGranted = isAccessibilityGranted,
            icon = Icons.Default.Accessibility,
            actionLabel = "Enable in Settings",
            onAction = onOpenAccessibility
        )

        Spacer(Modifier.height(12.dp))

        // 2. Microphone
        PermissionCard(
            title = "Microphone Access",
            description = "Required for hands-free voice commands and STT speech recognition.",
            isGranted = isMicGranted,
            icon = Icons.Default.Mic,
            actionLabel = "Grant Microphone",
            onAction = onRequestMic
        )

        Spacer(Modifier.height(12.dp))

        // 3. Draw Over Other Apps
        PermissionCard(
            title = "Floating Progress Overlay",
            description = "Shows live step execution overlay while other apps are active.",
            isGranted = isOverlayGranted,
            icon = Icons.Default.Layers,
            actionLabel = "Manage Overlay",
            onAction = onOpenOverlay
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisDarkSurface)
            .border(
                1.dp,
                if (isGranted) JarvisSuccess.copy(alpha = 0.7f) else JarvisGlassBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) JarvisSuccess else JarvisCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = JarvisSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = JarvisSuccess
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (!isGranted) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan.copy(alpha = 0.2f),
                        contentColor = JarvisCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
