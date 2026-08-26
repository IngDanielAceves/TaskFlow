package com.eduardogomez.taskflow.feature.home

import androidx.annotation.StringRes
import com.eduardogomez.taskflow.R
import com.eduardogomez.taskflow.data.local.TaskPriority

data class HomeUiState(
    val tasks: List<HomeTaskUiModel> = emptyList(),
    val selectedFilter: HomeFilter = HomeFilter.ALL,
    val pendingCount: Int = 0,
    val hasTasks: Boolean = false,
    val isLoading: Boolean = true,
)

data class HomeTaskUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val dueDateEpochDay: Long?,
    val dueTimeMinutes: Int?,
    val isCompleted: Boolean,
)

enum class HomeFilter(@param:StringRes val labelResId: Int) {
    ALL(R.string.filter_all),
    TODAY(R.string.filter_today),
    PENDING(R.string.filter_pending),
    COMPLETED(R.string.filter_completed),
}
