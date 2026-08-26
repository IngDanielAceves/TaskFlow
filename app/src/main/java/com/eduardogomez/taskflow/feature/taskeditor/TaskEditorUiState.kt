package com.eduardogomez.taskflow.feature.taskeditor

import com.eduardogomez.taskflow.data.local.TaskPriority
import java.time.LocalDate

data class TaskEditorUiState(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val dueTimeMinutes: Int? = null,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val saveError: Boolean = false,
)
