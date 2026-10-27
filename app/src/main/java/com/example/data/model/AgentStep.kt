package com.example.data.model

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

data class AgentStep(
    val index: Int = 0,
    val description: String = "",
    val status: StepStatus = StepStatus.PENDING,
    val errorMessage: String? = null
)
