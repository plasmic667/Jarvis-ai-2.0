package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId
    val userId: String = "",
    val displayName: String = "Jarvis User",
    val email: String = "",
    val modelTemperature: Double = 0.2,
    val voiceSpeed: Double = 1.0,
    val confirmationLevel: String = "MEDIUM",
    val wakeWordEnabled: Boolean = false,
    val pauseControl: Boolean = false,
    val voiceOutputEnabled: Boolean = true,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
