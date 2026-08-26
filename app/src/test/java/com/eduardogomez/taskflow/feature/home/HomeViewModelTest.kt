package com.eduardogomez.taskflow.feature.home

import com.eduardogomez.taskflow.data.local.TaskEntity
import com.eduardogomez.taskflow.data.local.TaskPriority
import com.eduardogomez.taskflow.data.repository.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun repositoryTasks_areExposedInUiState() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(listOf(task(id = 1), task(id = 2)))
        val viewModel = HomeViewModel(repository)

        val state = viewModel.awaitLoadedState()

        assertEquals(listOf(1L, 2L), state.tasks.map(HomeTaskUiModel::id))
        assertEquals("Task 1", state.tasks.first().title)
        assertFalse(state.isLoading)
        assertEquals(true, state.hasTasks)
    }

    @Test
    fun pendingCount_usesAllRepositoryTasks() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(
            listOf(
                task(id = 1, isCompleted = false),
                task(id = 2, isCompleted = true),
                task(id = 3, isCompleted = false),
            ),
        )
        val viewModel = HomeViewModel(repository)

        viewModel.selectFilter(HomeFilter.COMPLETED)
        val state = viewModel.awaitState(HomeFilter.COMPLETED)

        assertEquals(2, state.pendingCount)
        assertEquals(listOf(2L), state.tasks.map(HomeTaskUiModel::id))
    }

    @Test
    fun allFilter_showsEveryTask() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            FakeTaskRepository(listOf(task(id = 1), task(id = 2, isCompleted = true))),
        )

        val state = viewModel.awaitState(HomeFilter.ALL)

        assertEquals(listOf(1L, 2L), state.tasks.map(HomeTaskUiModel::id))
    }

    @Test
    fun todayFilter_showsOnlyTasksDueToday() = runTest(testDispatcher) {
        val today = LocalDate.now().toEpochDay()
        val viewModel = HomeViewModel(
            FakeTaskRepository(
                listOf(
                    task(id = 1, dueDateEpochDay = today),
                    task(id = 2, dueDateEpochDay = today + 1),
                    task(id = 3, dueDateEpochDay = null),
                ),
            ),
        )

        viewModel.selectFilter(HomeFilter.TODAY)
        val state = viewModel.awaitState(HomeFilter.TODAY)

        assertEquals(listOf(1L), state.tasks.map(HomeTaskUiModel::id))
    }

    @Test
    fun pendingFilter_showsOnlyIncompleteTasks() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            FakeTaskRepository(
                listOf(task(id = 1), task(id = 2, isCompleted = true), task(id = 3)),
            ),
        )

        viewModel.selectFilter(HomeFilter.PENDING)
        val state = viewModel.awaitState(HomeFilter.PENDING)

        assertEquals(listOf(1L, 3L), state.tasks.map(HomeTaskUiModel::id))
    }

    @Test
    fun completedFilter_showsOnlyCompletedTasks() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            FakeTaskRepository(
                listOf(task(id = 1), task(id = 2, isCompleted = true)),
            ),
        )

        viewModel.selectFilter(HomeFilter.COMPLETED)
        val state = viewModel.awaitState(HomeFilter.COMPLETED)

        assertEquals(listOf(2L), state.tasks.map(HomeTaskUiModel::id))
    }

    @Test
    fun setTaskCompleted_delegatesAndRepositoryEmissionUpdatesState() =
        runTest(testDispatcher) {
            val repository = FakeTaskRepository(listOf(task(id = 1)))
            val viewModel = HomeViewModel(repository)
            viewModel.awaitLoadedState()

            viewModel.setTaskCompleted(id = 1, isCompleted = true)
            advanceUntilIdle()
            val state = viewModel.uiState.first { currentState ->
                !currentState.isLoading &&
                    currentState.tasks.singleOrNull()?.isCompleted == true
            }

            assertEquals(listOf(1L to true), repository.completionUpdates)
            assertEquals(true, state.tasks.single().isCompleted)
            assertEquals(0, state.pendingCount)
        }

    private suspend fun HomeViewModel.awaitLoadedState(): HomeUiState =
        uiState.first { state -> !state.isLoading }

    private suspend fun HomeViewModel.awaitState(filter: HomeFilter): HomeUiState =
        uiState.first { state -> !state.isLoading && state.selectedFilter == filter }

    private fun task(
        id: Long,
        dueDateEpochDay: Long? = null,
        isCompleted: Boolean = false,
    ) = TaskEntity(
        id = id,
        title = "Task $id",
        priority = TaskPriority.MEDIUM,
        dueDateEpochDay = dueDateEpochDay,
        isCompleted = isCompleted,
        createdAtEpochMillis = id,
    )
}

private class FakeTaskRepository(initialTasks: List<TaskEntity>) : TaskRepository {
    private val tasks = MutableStateFlow(initialTasks)
    val completionUpdates = mutableListOf<Pair<Long, Boolean>>()

    override fun observeTasks(): Flow<List<TaskEntity>> = tasks

    override suspend fun getTask(id: Long): TaskEntity? =
        tasks.value.firstOrNull { task -> task.id == id }

    override suspend fun insertTask(task: TaskEntity): Long {
        tasks.value += task
        return task.id
    }

    override suspend fun updateTask(task: TaskEntity) {
        tasks.value = tasks.value.map { current ->
            if (current.id == task.id) task else current
        }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.value = tasks.value.filterNot { current -> current.id == task.id }
    }

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
        completionUpdates += id to isCompleted
        tasks.value = tasks.value.map { task ->
            if (task.id == id) task.copy(isCompleted = isCompleted) else task
        }
    }
}
