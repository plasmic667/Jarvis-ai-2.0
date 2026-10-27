package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserPreferencesRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(Firebase.firestore, Firebase.auth)

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun observeUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = getCurrentUserId()
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val settingsDocRef = firestore.collection("users").document(uid).collection("settings").document("preferences")
        val listener = settingsDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val temp = snapshot.getDouble("modelTemperature") ?: 0.2
                val speed = snapshot.getDouble("voiceSpeed") ?: 1.0
                val conf = snapshot.getString("confirmationLevel") ?: "MEDIUM"
                val wake = snapshot.getBoolean("wakeWordEnabled") ?: false
                val voiceOut = snapshot.getBoolean("voiceResponseEnabled") ?: true

                val profile = UserProfile(
                    userId = uid,
                    displayName = auth.currentUser?.displayName ?: "Jarvis User",
                    email = auth.currentUser?.email ?: "",
                    modelTemperature = temp,
                    voiceSpeed = speed,
                    confirmationLevel = conf,
                    wakeWordEnabled = wake,
                    voiceOutputEnabled = voiceOut
                )
                trySend(profile)
            } else {
                trySend(
                    UserProfile(
                        userId = uid,
                        displayName = auth.currentUser?.displayName ?: "Jarvis User",
                        email = auth.currentUser?.email ?: ""
                    )
                )
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = profile.userId.ifBlank { getCurrentUserId() } ?: return
        val settingsDocRef = firestore.collection("users").document(uid).collection("settings").document("preferences")

        val map = hashMapOf<String, Any>(
            "userId" to uid,
            "modelTemperature" to profile.modelTemperature,
            "voiceSpeed" to profile.voiceSpeed,
            "confirmationLevel" to profile.confirmationLevel,
            "wakeWordEnabled" to profile.wakeWordEnabled,
            "voiceResponseEnabled" to profile.voiceOutputEnabled,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        settingsDocRef.set(map).await()
    }
}
