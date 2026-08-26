package com.eduardogomez.taskflow.feature.taskeditor

import com.eduardogomez.taskflow.data.local.TaskPriority
import java.time.LocalDate

data class TaskEditorUiState(
    val mode: TaskEditorMode = TaskEditorMode.CREATE,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val dueTimeMinutes: Int? = null,
    val titleError: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val saveError: Boolean = false,
)

enum class TaskEditorMode {
    CREATE,
    EDIT,
}
