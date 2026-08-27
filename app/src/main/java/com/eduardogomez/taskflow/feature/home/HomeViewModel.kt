package com.eduardogomez.taskflow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eduardogomez.taskflow.data.local.TaskEntity
import com.eduardogomez.taskflow.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(HomeFilter.ALL)
    private val completionError = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.observeTasks(),
        selectedFilter,
        completionError,
    ) { tasks, filter, hasCompletionError ->
        val todayEpochDay = LocalDate.now().toEpochDay()
        val visibleTasks = tasks.filter { task -> filter.matches(task, todayEpochDay) }

        HomeUiState(
            tasks = visibleTasks.map(TaskEntity::toHomeTaskUiModel),
            selectedFilter = filter,
            pendingCount = tasks.count { task -> !task.isCompleted },
            hasTasks = tasks.isNotEmpty(),
            isLoading = false,
            completionError = hasCompletionError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = HomeUiState(),
    )

    fun selectFilter(filter: HomeFilter) {
        selectedFilter.value = filter
    }

    fun setTaskCompleted(id: Long, isCompleted: Boolean) {
        completionError.value = false
        viewModelScope.launch {
            try {
                taskRepository.setTaskCompleted(id, isCompleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                completionError.value = true
            }
        }
    }

    fun onCompletionErrorHandled() {
        completionError.value = false
    }
}

private fun HomeFilter.matches(task: TaskEntity, todayEpochDay: Long): Boolean = when (this) {
    HomeFilter.ALL -> true
    HomeFilter.TODAY -> task.dueDateEpochDay == todayEpochDay
    HomeFilter.PENDING -> !task.isCompleted
    HomeFilter.COMPLETED -> task.isCompleted
}

private fun TaskEntity.toHomeTaskUiModel() = HomeTaskUiModel(
    id = id,
    title = title,
    description = description,
    priority = priority,
    dueDateEpochDay = dueDateEpochDay,
    dueTimeMinutes = dueTimeMinutes,
    isCompleted = isCompleted,
)
