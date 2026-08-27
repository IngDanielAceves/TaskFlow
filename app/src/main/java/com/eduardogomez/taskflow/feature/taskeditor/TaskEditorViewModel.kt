package com.eduardogomez.taskflow.feature.taskeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduardogomez.taskflow.data.local.TaskEntity
import com.eduardogomez.taskflow.data.local.TaskPriority
import com.eduardogomez.taskflow.data.repository.TaskRepository
import com.eduardogomez.taskflow.navigation.TaskFlowDestination
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
    taskIdArgument: String?,
) : ViewModel() {
    @Inject
    constructor(
        taskRepository: TaskRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        taskRepository = taskRepository,
        currentTimeMillis = System::currentTimeMillis,
        todayEpochDay = LocalDate.now().toEpochDay(),
        taskIdArgument = savedStateHandle[TaskFlowDestination.TASK_ID_ARGUMENT],
    )

    private val taskId = taskIdArgument?.toLongOrNull()
    private val hasInvalidTaskId = taskIdArgument != null && taskId == null
    private var originalTask: TaskEntity? = null
    private val _uiState = MutableStateFlow(
        TaskEditorUiState(
            mode = if (taskIdArgument == null) TaskEditorMode.CREATE else TaskEditorMode.EDIT,
            dueDateEpochDay = todayEpochDay,
            isLoading = taskId != null,
            loadError = if (hasInvalidTaskId) {
                TaskEditorLoadError.INVALID_TASK_ID
            } else {
                null
            },
        ),
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null) {
            loadTask(taskId, todayEpochDay)
        }
    }

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

    fun onDueTimeCleared() {
        _uiState.update { state ->
            state.copy(dueTimeMinutes = null, saveError = false)
        }
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.isLoading || state.loadError != null || state.isSaving || state.isDeleting) {
            return
        }

        val title = state.title.trim()
        if (title.isEmpty()) {
            _uiState.update { currentState -> currentState.copy(titleError = true) }
            return
        }

        val task = when (state.mode) {
            TaskEditorMode.CREATE -> TaskEntity(
                id = 0,
                title = title,
                description = state.description.trim().ifEmpty { null },
                priority = state.priority,
                dueDateEpochDay = state.dueDateEpochDay,
                dueTimeMinutes = state.dueTimeMinutes,
                isCompleted = false,
                createdAtEpochMillis = currentTimeMillis(),
            )
            TaskEditorMode.EDIT -> originalTask?.copy(
                title = title,
                description = state.description.trim().ifEmpty { null },
                priority = state.priority,
                dueDateEpochDay = state.dueDateEpochDay,
                dueTimeMinutes = state.dueTimeMinutes,
            ) ?: return
        }

        _uiState.update { currentState ->
            currentState.copy(isSaving = true, saveError = false)
        }
        viewModelScope.launch {
            try {
                when (state.mode) {
                    TaskEditorMode.CREATE -> taskRepository.insertTask(task)
                    TaskEditorMode.EDIT -> taskRepository.updateTask(task)
                }
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

    fun onDeleteRequested() {
        val state = _uiState.value
        if (
            state.mode != TaskEditorMode.EDIT ||
            state.isLoading ||
            state.loadError != null ||
            state.isSaving ||
            state.isDeleting ||
            originalTask == null
        ) {
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isDeleteConfirmationVisible = true,
                deleteError = false,
            )
        }
    }

    fun onDeleteDismissed() {
        if (_uiState.value.isDeleting) return

        _uiState.update { state -> state.copy(isDeleteConfirmationVisible = false) }
    }

    fun onDeleteConfirmed() {
        val state = _uiState.value
        val task = originalTask
        if (
            state.mode != TaskEditorMode.EDIT ||
            !state.isDeleteConfirmationVisible ||
            state.isDeleting ||
            task == null
        ) {
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isDeleteConfirmationVisible = false,
                isDeleting = true,
                deleteError = false,
            )
        }
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
                _uiState.update { currentState ->
                    currentState.copy(isDeleting = false, deleteCompleted = true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(isDeleting = false, deleteError = true)
                }
            }
        }
    }

    fun onDeleteCompletedHandled() {
        _uiState.update { state -> state.copy(deleteCompleted = false) }
    }

    private fun loadTask(taskId: Long, todayEpochDay: Long) {
        viewModelScope.launch {
            try {
                val task = taskRepository.getTask(taskId)
                if (task == null) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = TaskEditorLoadError.NOT_FOUND,
                        )
                    }
                    return@launch
                }

                originalTask = task
                _uiState.update { state ->
                    state.copy(
                        title = task.title,
                        description = task.description.orEmpty(),
                        priority = task.priority,
                        dueDateEpochDay = task.dueDateEpochDay ?: todayEpochDay,
                        dueTimeMinutes = task.dueTimeMinutes,
                        isLoading = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        loadError = TaskEditorLoadError.LOAD_FAILED,
                    )
                }
            }
        }
    }
}
