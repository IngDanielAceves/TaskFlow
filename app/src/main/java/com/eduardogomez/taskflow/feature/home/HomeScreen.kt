package com.eduardogomez.taskflow.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eduardogomez.taskflow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTaskEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialTasks = fakeTasks()
    var tasks by remember(initialTasks) { mutableStateOf(initialTasks) }
    var selectedFilter by rememberSaveable { mutableStateOf(HomeFilter.ALL) }
    val pendingTaskCount = tasks.count { task -> !task.completed }
    val filteredTasks = remember(tasks, selectedFilter) {
        tasks.filter { task -> selectedFilter.matches(task) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_24),
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenTaskEditor) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_24),
                    contentDescription = stringResource(R.string.add_task),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HomeHeader(pendingTaskCount = pendingTaskCount)
            }

            item {
                FilterRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_tasks_for_filter),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            } else {
                items(
                    items = filteredTasks,
                    key = HomeTaskUiModel::id,
                ) { task ->
                    TaskCard(
                        task = task,
                        onCompletedChange = { completed ->
                            tasks = tasks.map { currentTask ->
                                if (currentTask.id == task.id) {
                                    currentTask.copy(completed = completed)
                                } else {
                                    currentTask
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    pendingTaskCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.tasks_remaining, pendingTaskCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun FilterRow(
    selectedFilter: HomeFilter,
    onFilterSelected: (HomeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = HomeFilter.entries,
            key = HomeFilter::name,
        ) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = stringResource(filter.labelResId)) },
            )
        }
    }
}

@Composable
private fun fakeTasks(): List<HomeTaskUiModel> = listOf(
    HomeTaskUiModel(
        id = 1,
        title = stringResource(R.string.task_android_course_title),
        description = stringResource(R.string.task_android_course_description),
        priority = TaskPriority.HIGH,
        due = stringResource(R.string.task_android_course_due),
        dueToday = true,
        completed = false,
    ),
    HomeTaskUiModel(
        id = 2,
        title = stringResource(R.string.task_english_title),
        description = stringResource(R.string.task_english_description),
        priority = TaskPriority.MEDIUM,
        due = stringResource(R.string.task_english_due),
        dueToday = true,
        completed = false,
    ),
    HomeTaskUiModel(
        id = 3,
        title = stringResource(R.string.task_interview_title),
        description = stringResource(R.string.task_interview_description),
        priority = TaskPriority.HIGH,
        due = stringResource(R.string.task_interview_due),
        dueToday = false,
        completed = false,
    ),
    HomeTaskUiModel(
        id = 4,
        title = stringResource(R.string.task_groceries_title),
        description = null,
        priority = TaskPriority.LOW,
        due = null,
        dueToday = false,
        completed = true,
    ),
)

internal data class HomeTaskUiModel(
    val id: Int,
    val title: String,
    val description: String?,
    val priority: TaskPriority,
    val due: String?,
    val dueToday: Boolean,
    val completed: Boolean,
)

internal enum class TaskPriority(@param:StringRes val labelResId: Int) {
    LOW(R.string.priority_low),
    MEDIUM(R.string.priority_medium),
    HIGH(R.string.priority_high),
}

private enum class HomeFilter(@param:StringRes val labelResId: Int) {
    ALL(R.string.filter_all),
    TODAY(R.string.filter_today),
    PENDING(R.string.filter_pending),
    COMPLETED(R.string.filter_completed),
    ;

    fun matches(task: HomeTaskUiModel): Boolean = when (this) {
        ALL -> true
        TODAY -> task.dueToday
        PENDING -> !task.completed
        COMPLETED -> task.completed
    }
}
