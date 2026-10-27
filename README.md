# Jarvis AI - Android Assistant

A personal AI phone control assistant built natively in Android with Kotlin and Jetpack Compose. Jarvis interprets natural language and voice commands, plans multi-step device actions, presents transparent real-time "thinking" step cards, and performs UI actions via an Android Accessibility Service while preserving user privacy and safety with explicit action confirmation.

## Features Ported & Implemented

- **Natural Language & Voice Interaction**: Real-time voice speech recognition via `JarvisSpeechManager` and conversational chat interface with Iron Man / Arc Reactor HUD aesthetics.
- **Agent Orchestrator & Task Planning**: `AgentOrchestrator` decomposes complex tasks into discrete `AgentStep` sequences and machine-executable `PhoneAction` operations.
- **Real-Time Step Tracking & Thinking Cards**: Interactive `ThinkingStepsCard` showing real-time step status (Pending, Running, Succeeded, Failed) alongside an Arc Reactor HUD visualizer.
- **Phone Automation via Accessibility Service**: `JarvisAccessibilityService` capable of app switching, text entry, tapping by content text/description, scrolling, dialing, and web searches.
- **Floating Task Progress Overlay**: Non-intrusive floating HUD overlay for visual progress feedback during multi-app workflows.
- **Cloud Persistence & Security Rules**: Firebase Firestore integration storing conversation history (`/users/{uid}/messages`), task logs (`/users/{uid}/tasks`), and customizable preferences (`/users/{uid}/settings`).
- **Safety Confirmation Gates**: Interactive `ConfirmationDialog` guarding sensitive actions (calls, messaging, and multi-step configurations) according to configurable risk levels (Low, Medium, High).
- **Google Authentication & Onboarding**: Seamless Google Sign-In with Jetpack `CredentialManager` and permission onboarding slides.

## Tech Stack

- **UI**: Android Jetpack Compose with Material Design 3 and Iron Man HUD glassmorphism styling
- **Language**: Kotlin 2.x with Coroutines & StateFlow
- **AI & Cloud Backend**: Firebase AI Logic (Gemini API) and Firebase Cloud Firestore
- **Hardware & Services**: Android Accessibility Service, Android SpeechRecognizer, TextToSpeech
