package com.example.data.repository

import android.content.Context
import com.example.data.model.TaskRecord
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

class TaskHistoryRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(Firebase.firestore, Firebase.auth)

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun saveTask(task: TaskRecord) {
        val uid = task.userId.ifBlank { getCurrentUserId() }
        val docRef = firestore.collection("users").document(uid).collection("tasks").document(task.id)

        val map = hashMapOf<String, Any>(
            "id" to task.id,
            "userId" to uid,
            "command" to task.command,
            "status" to task.status,
            "createdAt" to (task.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.stepsSummary.isNotBlank()) {
            map["stepsSummary"] = task.stepsSummary
        }
        docRef.set(map).await()
    }

    fun observeTaskHistory(): Flow<List<TaskRecord>> = callbackFlow {
        val uid = getCurrentUserId()
        val query = firestore.collection("users").document(uid).collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(TaskRecord::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }
}
