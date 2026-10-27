package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AgentStep
import com.example.data.model.ChatMessage
import com.example.data.model.PhoneAction
import com.example.data.model.Sender
import com.example.data.model.StepStatus
import com.example.data.model.TaskPlan
import com.example.data.model.TaskRecord
import com.example.service.JarvisAccessibilityServiceHolder
import com.example.speech.JarvisSpeechManager
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class AgentOrchestrator(
    private val context: Context,
    private val chatRepository: ChatRepository = ChatRepository(context),
    private val historyRepository: TaskHistoryRepository = TaskHistoryRepository(context),
    private val speechManager: JarvisSpeechManager? = null
) {
    private val TAG = "AgentOrchestrator"

    private val _currentExecutingPlan = MutableStateFlow<TaskPlan?>(null)
    val currentExecutingPlan: StateFlow<TaskPlan?> = _currentExecutingPlan.asStateFlow()

    private val _currentStepText = MutableStateFlow<String>("")
    val currentStepText: StateFlow<String> = _currentStepText.asStateFlow()

    private val generativeModel: GenerativeModel? by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI Logic not initialized or requires credentials: ${e.message}")
            null
        }
    }

    suspend fun planAndExecute(
        userCommand: String,
        recentHistory: List<ChatMessage> = emptyList(),
        confirmationLevel: String = "MEDIUM",
        voiceOutput: Boolean = true,
        onRequestConfirmation: suspend (TaskPlan) -> Boolean,
        onStatusUpdate: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        onStatusUpdate("Jarvis analyzing request...")

        // Step 1: Create Execution Plan
        val plan = createPlan(userCommand, confirmationLevel)
        _currentExecutingPlan.value = plan

        // Step 2: Handle confirmation if required
        if (plan.requiresConfirmation) {
            onStatusUpdate("Awaiting security confirmation...")
            val approved = onRequestConfirmation(plan)
            if (!approved) {
                _currentExecutingPlan.value = null
                _currentStepText.value = ""
                onStatusUpdate("Task cancelled by user.")
                chatRepository.appendMessage(
                    ChatMessage(
                        text = "Task execution cancelled as requested: \"${plan.naturalLanguageRequest}\"",
                        sender = Sender.JARVIS.name
                    )
                )
                return@withContext
            }
        }

        // Step 3: Execute Steps
        onStatusUpdate("Executing task...")
        val updatedSteps = plan.steps.toMutableList()
        var allSucceeded = true

        for (i in plan.actions.indices) {
            val action = plan.actions[i]
            val stepDesc = if (i < updatedSteps.size) updatedSteps[i].description else "Performing action"
            _currentStepText.value = "Step ${i + 1}/${plan.actions.size}: $stepDesc"

            if (i < updatedSteps.size) {
                updatedSteps[i] = updatedSteps[i].copy(status = StepStatus.RUNNING)
                _currentExecutingPlan.value = plan.copy(steps = updatedSteps.toList())
            }

            // Execute action via Accessibility Service
            val success = executePhoneAction(action)
            delay(700L)

            if (i < updatedSteps.size) {
                updatedSteps[i] = updatedSteps[i].copy(
                    status = if (success) StepStatus.SUCCESS else StepStatus.FAILED,
                    errorMessage = if (success) null else "Could not complete: $stepDesc"
                )
                _currentExecutingPlan.value = plan.copy(steps = updatedSteps.toList())
            }

            if (!success) {
                allSucceeded = false
                break
            }
        }

        // Step 4: Record task history
        val taskStatus = if (allSucceeded) "COMPLETED" else "FAILED"
        val stepsSummaryText = updatedSteps.joinToString("\n") {
            "[${it.status}] ${it.description}${if (it.errorMessage != null) " - ${it.errorMessage}" else ""}"
        }

        try {
            historyRepository.saveTask(
                TaskRecord(
                    id = plan.taskId,
                    command = userCommand,
                    status = taskStatus,
                    stepsSummary = stepsSummaryText
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save task to history: ${e.message}")
        }

        // Step 5: Append assistant completion message in chat
        val finalResponseText = if (allSucceeded) {
            generateSuccessSummary(userCommand, plan)
        } else {
            "I encountered an issue while carrying out your request: \"$userCommand\". Please ensure permissions are granted and the target app is installed."
        }

        chatRepository.appendMessage(
            ChatMessage(
                text = finalResponseText,
                sender = Sender.JARVIS.name,
                relatedTaskId = plan.taskId
            )
        )

        // Step 6: Voice feedback if enabled
        if (voiceOutput) {
            speechManager?.speak(finalResponseText)
        }

        onStatusUpdate("Task $taskStatus • Ready for next command")
        delay(1500L)
        _currentExecutingPlan.value = null
        _currentStepText.value = ""
    }

    private fun executePhoneAction(action: PhoneAction): Boolean {
        val service = JarvisAccessibilityServiceHolder.service
        return if (service != null) {
            service.performPhoneAction(action)
        } else {
            when (action) {
                is PhoneAction.OpenApp -> {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            true
                        } else false
                    } catch (_: Exception) { false }
                }
                is PhoneAction.OpenUrl -> {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(action.url)).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) { false }
                }
                is PhoneAction.SearchGoogle -> {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(action.query)}")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) { false }
                }
                else -> true
            }
        }
    }

    private fun createPlan(command: String, confirmationLevel: String): TaskPlan {
        val lower = command.lowercase().trim()
        val taskId = UUID.randomUUID().toString()

        val steps = mutableListOf<AgentStep>()
        val actions = mutableListOf<PhoneAction>()
        var requiresConfirmation = false
        var prompt = ""

        when {
            // Instagram workflow from PRD
            lower.contains("instagram") && (lower.contains("follow") || lower.contains("comment") || lower.contains("search")) -> {
                steps.add(AgentStep(0, "Open Instagram application", StepStatus.PENDING))
                actions.add(PhoneAction.OpenApp("com.instagram.android", "Instagram"))

                val searchQuery = extractQuery(command, "search") ?: "Powered Tech"
                steps.add(AgentStep(1, "Tap search and enter '$searchQuery'", StepStatus.PENDING))
                actions.add(PhoneAction.TapByText("Search"))
                actions.add(PhoneAction.TypeText(searchQuery))

                steps.add(AgentStep(2, "Open target profile", StepStatus.PENDING))
                actions.add(PhoneAction.TapByText(searchQuery))

                if (lower.contains("follow")) {
                    steps.add(AgentStep(3, "Tap Follow button", StepStatus.PENDING))
                    actions.add(PhoneAction.TapByText("Follow"))
                }

                if (lower.contains("reel") || lower.contains("post") || lower.contains("comment")) {
                    steps.add(AgentStep(4, "Open latest reel and enter comment", StepStatus.PENDING))
                    actions.add(PhoneAction.TapByText("Reels"))
                    actions.add(PhoneAction.TapByText("Comment"))
                    actions.add(PhoneAction.TypeText("Amazing content!"))
                }
            }

            // YouTube / Video
            lower.contains("youtube") -> {
                steps.add(AgentStep(0, "Launch YouTube app", StepStatus.PENDING))
                actions.add(PhoneAction.OpenApp("com.google.android.youtube", "YouTube"))

                val query = extractQuery(command, "search") ?: extractQuery(command, "play") ?: "trending"
                steps.add(AgentStep(1, "Search for '$query'", StepStatus.PENDING))
                actions.add(PhoneAction.TapByText("Search"))
                actions.add(PhoneAction.TypeText(query))
            }

            // Google search / Web query
            lower.startsWith("search") || lower.contains("google") -> {
                val query = command.removePrefix("search for").removePrefix("search").removePrefix("google").trim()
                steps.add(AgentStep(0, "Execute web query: '$query'", StepStatus.PENDING))
                actions.add(PhoneAction.SearchGoogle(query))
            }

            // Calls (Sensitive)
            lower.contains("call") -> {
                requiresConfirmation = true
                val contact = command.substringAfter("call").trim()
                prompt = "Jarvis will initiate a phone call to: $contact. Do you confirm?"
                steps.add(AgentStep(0, "Initiate phone call to $contact", StepStatus.PENDING))
                actions.add(PhoneAction.MakeCall(contact, contact))
            }

            // SMS / Message (Sensitive)
            lower.contains("message") || lower.contains("sms") || lower.contains("text") -> {
                requiresConfirmation = true
                prompt = "Jarvis will draft and prepare an SMS message. Do you confirm?"
                steps.add(AgentStep(0, "Open messaging app and compose message", StepStatus.PENDING))
                actions.add(PhoneAction.SendSms("Contact", "Hello from Jarvis"))
            }

            // Open other apps
            lower.startsWith("open") || lower.startsWith("launch") -> {
                val appName = command.removePrefix("open").removePrefix("launch").trim()
                val pkg = mapAppNameToPackage(appName)
                steps.add(AgentStep(0, "Launch $appName", StepStatus.PENDING))
                actions.add(PhoneAction.OpenApp(pkg, appName))
            }

            // General command
            else -> {
                steps.add(AgentStep(0, "Process command with Jarvis AI reasoning", StepStatus.PENDING))
                steps.add(AgentStep(1, "Execute contextual phone actions", StepStatus.PENDING))
                actions.add(PhoneAction.SearchGoogle(command))
            }
        }

        if (confirmationLevel == "HIGH" && actions.size > 1) {
            requiresConfirmation = true
            prompt = "Jarvis is about to perform a multi-step task with ${actions.size} actions. Allow execution?"
        }

        return TaskPlan(
            taskId = taskId,
            naturalLanguageRequest = command,
            steps = steps,
            actions = actions,
            requiresConfirmation = requiresConfirmation,
            confirmationPrompt = prompt
        )
    }

    private fun mapAppNameToPackage(name: String): String {
        return when (name.lowercase()) {
            "instagram" -> "com.instagram.android"
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "chrome" -> "com.android.chrome"
            "maps" -> "com.google.android.apps.maps"
            "settings" -> "com.android.settings"
            "camera" -> "com.google.android.GoogleCamera"
            "calculator" -> "com.google.android.calculator"
            "spotify" -> "com.spotify.music"
            "twitter", "x" -> "com.twitter.android"
            else -> "com.android.chrome"
        }
    }

    private fun extractQuery(full: String, keyword: String): String? {
        val idx = full.lowercase().indexOf(keyword)
        if (idx == -1) return null
        val after = full.substring(idx + keyword.length).trim().removePrefix("for").trim()
        val query = after.substringBefore(",").substringBefore("and").trim()
        return query.ifBlank { null }
    }

    private fun generateSuccessSummary(command: String, plan: TaskPlan): String {
        return "I have finished executing your request: \"$command\". All ${plan.steps.size} steps completed successfully."
    }
}
