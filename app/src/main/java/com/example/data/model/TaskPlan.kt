package com.example.data.model

import java.util.UUID

data class TaskPlan(
    val taskId: String = UUID.randomUUID().toString(),
    val naturalLanguageRequest: String = "",
    val steps: List<AgentStep> = emptyList(),
    val actions: List<PhoneAction> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String = ""
)
