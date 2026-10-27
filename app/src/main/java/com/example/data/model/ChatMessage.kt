package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

enum class Sender {
    USER,
    JARVIS,
    SYSTEM
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val text: String = "",
    val sender: String = Sender.USER.name,
    val relatedTaskId: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    @get:Exclude
    var thinkingSteps: List<AgentStep> = emptyList(),
    @get:Exclude
    var isStreaming: Boolean = false
) {
    val senderEnum: Sender
        get() = try {
            Sender.valueOf(sender)
        } catch (_: Exception) {
            Sender.JARVIS
        }
}
