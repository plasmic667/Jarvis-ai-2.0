package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChatMessage
import com.example.data.model.TaskRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class JarvisRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveTask_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = JarvisRepository(firestore)

        val taskId = UUID.randomUUID().toString()
        val task = TaskRecord(
            id = taskId,
            userId = uid,
            command = "Open Instagram and comment",
            status = "PENDING"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveTask(task)
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(observed.any { it.id == taskId })
    }

    @Test
    fun appendMessage_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = JarvisRepository(firestore)

        val msgId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = msgId,
            userId = uid,
            text = "Hey Jarvis, search YouTube",
            sender = "USER"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendMessage(message)
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages().first { list -> list.any { it.id == msgId } }
        }
        assertTrue(observed.any { it.id == msgId })
    }

    @Test
    fun observeTasks_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = JarvisRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
            fail("Expected exception for unauthenticated access")
        } catch (e: Exception) {
            caughtException = e
        }
        val firestoreException = caughtException?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun crossUserTaskAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = JarvisRepository(firestore)
        val taskId = UUID.randomUUID().toString()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveTask(
                TaskRecord(
                    id = taskId,
                    userId = aliceUid,
                    command = "Secret Task",
                    status = "PENDING"
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        var caughtException: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("tasks").document(taskId).get().await()
            }
            fail("Expected exception for cross user task access")
        } catch (e: FirebaseFirestoreException) {
            caughtException = e
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caughtException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@jarvis.ai"
        const val BOB_EMAIL = "bob@jarvis.ai"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
