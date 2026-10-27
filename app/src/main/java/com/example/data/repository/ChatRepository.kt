package com.example.data.repository

import android.content.Context
import com.example.data.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(Firebase.firestore, Firebase.auth)

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun appendMessage(message: ChatMessage): String {
        val uid = message.userId.ifBlank { getCurrentUserId() }
        val msgId = message.id.ifBlank { java.util.UUID.randomUUID().toString() }
        val docRef = firestore.collection("users").document(uid).collection("messages").document(msgId)

        val map = hashMapOf<String, Any>(
            "id" to msgId,
            "userId" to uid,
            "text" to message.text,
            "sender" to message.sender,
            "createdAt" to (message.createdAt ?: FieldValue.serverTimestamp())
        )
        if (message.relatedTaskId != null) {
            map["relatedTaskId"] = message.relatedTaskId
        }
        docRef.set(map).await()
        return msgId
    }

    fun observeChats(): Flow<List<ChatMessage>> = callbackFlow {
        val uid = getCurrentUserId()
        val query = firestore.collection("users").document(uid).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun clearHistory() {
        val uid = getCurrentUserId()
        val snapshot = firestore.collection("users").document(uid).collection("messages").get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
