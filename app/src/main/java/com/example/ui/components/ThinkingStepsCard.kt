package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentStep
import com.example.data.model.StepStatus
import com.example.data.model.TaskPlan
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisError
import com.example.ui.theme.JarvisGlassBorder
import com.example.ui.theme.JarvisGlassCard
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ThinkingStepsCard(
    plan: TaskPlan,
    modifier: Modifier = Modifier
) {
    val completedCount = plan.steps.count { it.status == StepStatus.SUCCESS }
    val totalCount = plan.steps.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisGlassCard.copy(alpha = 0.85f))
            .border(1.dp, JarvisGlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .animateContentSize()
            .testTag("thinking_steps_card")
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Thinking",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "JARVIS EXECUTION PLAN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = JarvisCyan
                    )
                }

                Text(
                    text = "$completedCount/$totalCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (completedCount == totalCount && totalCount > 0) JarvisSuccess else TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Steps list
            plan.steps.forEachIndexed { idx, step ->
                StepRow(index = idx + 1, step = step)
                if (idx < plan.steps.size - 1) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    step: AgentStep
) {
    val statusColor = when (step.status) {
        StepStatus.PENDING -> TextMuted
        StepStatus.RUNNING -> JarvisCyan
        StepStatus.SUCCESS -> JarvisSuccess
        StepStatus.FAILED -> JarvisError
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                StepStatus.PENDING -> {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextMuted
                    )
                }
                StepStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = JarvisCyan
                    )
                }
                StepStatus.SUCCESS -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = JarvisSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                }
                StepStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = JarvisError,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (step.status == StepStatus.RUNNING) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = when (step.status) {
                    StepStatus.PENDING -> TextSecondary
                    StepStatus.RUNNING -> TextPrimary
                    StepStatus.SUCCESS -> TextPrimary.copy(alpha = 0.9f)
                    StepStatus.FAILED -> JarvisError
                }
            )
            AnimatedVisibility(visible = step.errorMessage != null) {
                step.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisError.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
