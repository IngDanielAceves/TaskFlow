package com.eduardogomez.taskflow.feature.taskeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduardogomez.taskflow.data.local.TaskEntity
import com.eduardogomez.taskflow.data.local.TaskPriority
import com.eduardogomez.taskflow.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TaskEditorViewModel internal constructor(
    private val taskRepository: TaskRepository,
    private val currentTimeMillis: () -> Long,
    todayEpochDay: Long,
) : ViewModel() {
    @Inject
    constructor(taskRepository: TaskRepository) : this(
        taskRepository = taskRepository,
        currentTimeMillis = System::currentTimeMillis,
        todayEpochDay = LocalDate.now().toEpochDay(),
    )

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(dueDateEpochDay = todayEpochDay),
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    fun onTitleChanged(title: String) {
        _uiState.update { state ->
            state.copy(
                title = title,
                titleError = state.titleError && title.isBlank(),
                saveError = false,
            )
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { state -> state.copy(description = description, saveError = false) }
    }

    fun onPriorityChanged(priority: TaskPriority) {
        _uiState.update { state -> state.copy(priority = priority, saveError = false) }
    }

    fun onDueDateChanged(dueDateEpochDay: Long) {
        _uiState.update { state ->
            state.copy(dueDateEpochDay = dueDateEpochDay, saveError = false)
        }
    }

    fun onDueTimeChanged(dueTimeMinutes: Int) {
        _uiState.update { state ->
            state.copy(dueTimeMinutes = dueTimeMinutes, saveError = false)
        }
    }

    fun createTask() {
        val state = _uiState.value
        if (state.isSaving) return

        val title = state.title.trim()
        if (title.isEmpty()) {
            _uiState.update { currentState -> currentState.copy(titleError = true) }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(isSaving = true, saveError = false)
        }
        viewModelScope.launch {
            try {
                taskRepository.insertTask(
                    TaskEntity(
                        id = 0,
                        title = title,
                        description = state.description.trim().ifEmpty { null },
                        priority = state.priority,
                        dueDateEpochDay = state.dueDateEpochDay,
                        dueTimeMinutes = state.dueTimeMinutes,
                        isCompleted = false,
                        createdAtEpochMillis = currentTimeMillis(),
                    ),
                )
                _uiState.update { currentState ->
                    currentState.copy(isSaving = false, saveCompleted = true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(isSaving = false, saveError = true)
                }
            }
        }
    }

    fun onSaveCompletedHandled() {
        _uiState.update { state -> state.copy(saveCompleted = false) }
    }
}
