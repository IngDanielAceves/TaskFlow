<h1 align="center">✅ TaskFlow</h1>

<p align="center">
  A modern Android task manager built with Kotlin and Jetpack Compose.
</p>

<p align="center">
  Local-first · Reactive · Tested · Built with modern Android architecture
</p>

📱 Preview

<p align="center">
  <img src="docs/screenshots/HOME.png" width="215" alt="TaskFlow Home"/>
  <img src="docs/screenshots/new-task.png" width="215" alt="Create Task"/>
  <img src="docs/screenshots/edit-task.png" width="215" alt="Edit Task"/>
  <img src="docs/screenshots/dark-mode.png" width="215" alt="TaskFlow Dark Mode"/>
</p>

✨ Features

Create, edit and delete tasks

Mark tasks as completed or pending

Low, Medium and High priority levels

Due date and optional due time

Filters for All, Today, Pending and Completed

Light and Dark Theme

Local persistence with Room

Validation, empty states and basic error handling

Delete confirmation for destructive actions

🛠 Tech Stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Coroutines · Flow / StateFlow · Navigation Compose · Room · Hilt · KSP

🧱 Architecture

TaskFlow uses a simple architecture intentionally sized for the project:

Compose UI
↓
ViewModel
↓
Repository
↓
Room DAO
↓
Room Database

Room acts as the local source of truth. ViewModels expose immutable StateFlow state, and Compose observes it with lifecycle-aware collection.

Because Room queries expose Flow, changes made through Create, Update, Delete or completion actions automatically propagate back to Home without a manual refresh.

🔄 Reactive Flow

Room
↓
TaskRepository
↓
ViewModel
↓
StateFlow<UiState>
↓
Compose

The same TaskEditor is reused for both Create and Edit modes. Navigation passes an optional task ID, which is handled through SavedStateHandle.

🧪 Testing

TaskFlow includes tests at multiple layers:

41 unit tests for ViewModel state and behavior

Room DAO tests using an in-memory database

8 instrumented tests covering Room and Compose/Navigation integration

0 test failures

0 lint errors

The test strategy focuses on behavior rather than annotation or implementation-detail testing.

🚀 Getting Started

Clone

git clone https://github.com/IngDanielAceves/TaskFlow.git
cd TaskFlow

Build

./gradlew assembleDebug

Unit tests

./gradlew testDebugUnitTest

Lint

./gradlew lintDebug

Instrumented tests

Requires an Android emulator or physical device:

./gradlew connectedDebugAndroidTest

⚙️ Requirements

Android Studio

JDK 21

Minimum Android version: API 26

Android SDK compatible with the project's configured compileSdk

📌 Project Status

MVP complete.

TaskFlow was built to demonstrate practical Android fundamentals with a focused scope rather than unnecessary architectural complexity.

Potential future improvements include backend synchronization, Room migrations for future schema changes, process-death restoration for unsaved drafts, and CI automation.

👨‍💻 Author

Eduardo Gómez
Android Developer · Kotlin · Jetpack Compose

GitHub: @IngDanielAceves