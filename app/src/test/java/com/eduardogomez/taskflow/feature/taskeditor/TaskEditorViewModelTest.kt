package com.eduardogomez.taskflow.feature.taskeditor

import com.eduardogomez.taskflow.data.local.TaskEntity
import com.eduardogomez.taskflow.data.local.TaskPriority
import com.eduardogomez.taskflow.data.repository.TaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskEditorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val todayEpochDay = 20_000L
    private val timestamp = 1_234_567L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = createViewModel(FakeTaskRepository()).uiState.value

        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals(TaskPriority.MEDIUM, state.priority)
        assertEquals(todayEpochDay, state.dueDateEpochDay)
        assertNull(state.dueTimeMinutes)
        assertFalse(state.titleError)
        assertFalse(state.isSaving)
    }

    @Test
    fun onTitleChanged_updatesTitleAndClearsValidationWhenValid() {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.createTask()
        assertTrue(viewModel.uiState.value.titleError)

        viewModel.onTitleChanged("Learn StateFlow")

        assertEquals("Learn StateFlow", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.titleError)
    }

    @Test
    fun onDescriptionChanged_updatesDescription() {
        val viewModel = createViewModel(FakeTaskRepository())

        viewModel.onDescriptionChanged("Review the documentation")

        assertEquals("Review the documentation", viewModel.uiState.value.description)
    }

    @Test
    fun onPriorityChanged_updatesPriority() {
        val viewModel = createViewModel(FakeTaskRepository())

        viewModel.onPriorityChanged(TaskPriority.HIGH)

        assertEquals(TaskPriority.HIGH, viewModel.uiState.value.priority)
    }

    @Test
    fun dateAndTimeChanges_updateRawValues() {
        val viewModel = createViewModel(FakeTaskRepository())

        viewModel.onDueDateChanged(todayEpochDay + 3)
        viewModel.onDueTimeChanged(19 * 60 + 30)

        assertEquals(todayEpochDay + 3, viewModel.uiState.value.dueDateEpochDay)
        assertEquals(1_170, viewModel.uiState.value.dueTimeMinutes)
    }

    @Test
    fun emptyTitle_doesNotInsert() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("   ")

        viewModel.createTask()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.titleError)
        assertTrue(repository.insertedTasks.isEmpty())
    }

    @Test
    fun validForm_insertsExpectedTaskEntity() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("  Learn Room  ")
        viewModel.onDescriptionChanged("  Persist a task  ")
        viewModel.onPriorityChanged(TaskPriority.HIGH)
        viewModel.onDueDateChanged(todayEpochDay + 1)
        viewModel.onDueTimeChanged(8 * 60 + 15)

        viewModel.createTask()
        advanceUntilIdle()

        assertEquals(
            TaskEntity(
                id = 0,
                title = "Learn Room",
                description = "Persist a task",
                priority = TaskPriority.HIGH,
                dueDateEpochDay = todayEpochDay + 1,
                dueTimeMinutes = 495,
                isCompleted = false,
                createdAtEpochMillis = timestamp,
            ),
            repository.insertedTasks.single(),
        )
    }

    @Test
    fun blankDescription_isInsertedAsNull() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("Task without description")
        viewModel.onDescriptionChanged("   ")

        viewModel.createTask()
        advanceUntilIdle()

        assertNull(repository.insertedTasks.single().description)
    }

    @Test
    fun successfulInsert_signalsNavigation() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.onTitleChanged("Created task")

        viewModel.createTask()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveCompleted)
        viewModel.onSaveCompletedHandled()
        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun repeatedCreateWhileSaving_insertsOnlyOnce() = runTest(testDispatcher) {
        val insertGate = CompletableDeferred<Unit>()
        val repository = FakeTaskRepository(insertGate = insertGate)
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("Only once")

        viewModel.createTask()
        viewModel.createTask()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(1, repository.insertedTasks.size)

        insertGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.insertedTasks.size)
        assertTrue(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun repositoryFailure_doesNotSignalNavigation() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(
            insertFailure = IllegalStateException("Database unavailable"),
        )
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("Retry later")

        viewModel.createTask()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.saveCompleted)
        assertTrue(viewModel.uiState.value.saveError)
    }

    private fun createViewModel(repository: TaskRepository) = TaskEditorViewModel(
        taskRepository = repository,
        currentTimeMillis = { timestamp },
        todayEpochDay = todayEpochDay,
    )
}

private class FakeTaskRepository(
    private val insertGate: CompletableDeferred<Unit>? = null,
    private val insertFailure: Exception? = null,
) : TaskRepository {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val insertedTasks = mutableListOf<TaskEntity>()

    override fun observeTasks(): Flow<List<TaskEntity>> = tasks

    override suspend fun getTask(id: Long): TaskEntity? =
        tasks.value.firstOrNull { task -> task.id == id }

    override suspend fun insertTask(task: TaskEntity): Long {
        insertedTasks += task
        insertGate?.await()
        insertFailure?.let { throw it }
        return 1L
    }

    override suspend fun updateTask(task: TaskEntity) = Unit

    override suspend fun deleteTask(task: TaskEntity) = Unit

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) = Unit
}
