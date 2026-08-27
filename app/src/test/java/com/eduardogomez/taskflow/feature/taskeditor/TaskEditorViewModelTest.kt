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

        assertEquals(TaskEditorMode.CREATE, state.mode)
        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals(TaskPriority.MEDIUM, state.priority)
        assertEquals(todayEpochDay, state.dueDateEpochDay)
        assertNull(state.dueTimeMinutes)
        assertFalse(state.titleError)
        assertFalse(state.isSaving)
        assertFalse(state.isDeleteConfirmationVisible)
        assertFalse(state.isDeleting)
        assertFalse(state.deleteCompleted)
    }

    @Test
    fun onTitleChanged_updatesTitleAndClearsValidationWhenValid() {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.saveTask()
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
    fun pastDueDate_isIgnored() {
        val viewModel = createViewModel(FakeTaskRepository())

        viewModel.onDueDateChanged(todayEpochDay - 1)

        assertEquals(todayEpochDay, viewModel.uiState.value.dueDateEpochDay)
    }

    @Test
    fun dueTimeOutsideValidRange_isIgnored() {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.onDueTimeChanged(9 * 60)

        viewModel.onDueTimeChanged(-1)
        viewModel.onDueTimeChanged(24 * 60)

        assertEquals(9 * 60, viewModel.uiState.value.dueTimeMinutes)
    }

    @Test
    fun dueTimeCanBeClearedAfterBeingSet() {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.onDueTimeChanged(9 * 60 + 30)

        viewModel.onDueTimeCleared()

        assertNull(viewModel.uiState.value.dueTimeMinutes)
    }

    @Test
    fun emptyTitle_doesNotInsert() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)
        viewModel.onTitleChanged("   ")

        viewModel.saveTask()
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

        viewModel.saveTask()
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

        viewModel.saveTask()
        advanceUntilIdle()

        assertNull(repository.insertedTasks.single().description)
    }

    @Test
    fun successfulInsert_signalsNavigation() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.onTitleChanged("Created task")

        viewModel.saveTask()
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

        viewModel.saveTask()
        viewModel.saveTask()
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

        viewModel.saveTask()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.saveCompleted)
        assertTrue(viewModel.uiState.value.saveError)
    }

    @Test
    fun existingTask_loadsAllEditableFields() = runTest(testDispatcher) {
        val task = existingTask()
        val viewModel = createViewModel(
            repository = FakeTaskRepository(initialTasks = listOf(task)),
            taskId = task.id,
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(TaskEditorMode.EDIT, state.mode)
        assertFalse(state.isLoading)
        assertEquals(task.title, state.title)
        assertEquals(task.description, state.description)
        assertEquals(task.priority, state.priority)
        assertEquals(task.dueDateEpochDay, state.dueDateEpochDay)
        assertEquals(task.dueTimeMinutes, state.dueTimeMinutes)
    }

    @Test
    fun edit_preservesOriginalId() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onTitleChanged("Updated")
        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals(task.id, repository.updatedTasks.single().id)
    }

    @Test
    fun edit_preservesOriginalCreatedAtTimestamp() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.saveTask()
        advanceUntilIdle()

        assertEquals(
            task.createdAtEpochMillis,
            repository.updatedTasks.single().createdAtEpochMillis,
        )
    }

    @Test
    fun edit_preservesOriginalCompletionState() = runTest(testDispatcher) {
        val task = existingTask(isCompleted = true)
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(repository.updatedTasks.single().isCompleted)
    }

    @Test
    fun edit_usesUpdateAndDoesNotInsert() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onTitleChanged("Updated title")
        viewModel.onPriorityChanged(TaskPriority.LOW)
        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(repository.insertedTasks.isEmpty())
        assertEquals(1, repository.updatedTasks.size)
        assertEquals("Updated title", repository.updatedTasks.single().title)
        assertEquals(TaskPriority.LOW, repository.updatedTasks.single().priority)
    }

    @Test
    fun successfulUpdate_signalsNavigation() = runTest(testDispatcher) {
        val task = existingTask()
        val viewModel = createViewModel(
            repository = FakeTaskRepository(initialTasks = listOf(task)),
            taskId = task.id,
        )
        advanceUntilIdle()

        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun failedUpdate_doesNotSignalNavigation() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(
            initialTasks = listOf(task),
            updateFailure = IllegalStateException("Database unavailable"),
        )
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.saveTask()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveCompleted)
        assertTrue(viewModel.uiState.value.saveError)
    }

    @Test
    fun missingTask_doesNotInsertOrUpdate() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository, taskId = 404)
        advanceUntilIdle()

        assertEquals(TaskEditorMode.EDIT, viewModel.uiState.value.mode)
        assertEquals(TaskEditorLoadError.NOT_FOUND, viewModel.uiState.value.loadError)
        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(repository.insertedTasks.isEmpty())
        assertTrue(repository.updatedTasks.isEmpty())
        assertFalse(viewModel.uiState.value.saveCompleted)
    }

    @Test
    fun loadFailure_isDifferentFromMissingTask() = runTest(testDispatcher) {
        val repository = FakeTaskRepository(
            getFailure = IllegalStateException("Database unavailable"),
        )
        val viewModel = createViewModel(repository, taskId = 42)
        advanceUntilIdle()

        assertEquals(TaskEditorMode.EDIT, viewModel.uiState.value.mode)
        assertEquals(TaskEditorLoadError.LOAD_FAILED, viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun invalidTaskId_isAnErrorInsteadOfCreateMode() {
        val viewModel = createViewModel(
            repository = FakeTaskRepository(),
            taskIdArgument = "not-a-number",
        )

        assertEquals(TaskEditorMode.EDIT, viewModel.uiState.value.mode)
        assertEquals(TaskEditorLoadError.INVALID_TASK_ID, viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun emptyTitleInEditMode_doesNotUpdate() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onTitleChanged("   ")
        viewModel.saveTask()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.titleError)
        assertTrue(repository.updatedTasks.isEmpty())
    }

    @Test
    fun deleteIsUnavailableInCreateMode() = runTest(testDispatcher) {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertTrue(repository.deletedTasks.isEmpty())
    }

    @Test
    fun deleteRequestInEditMode_showsConfirmation() = runTest(testDispatcher) {
        val task = existingTask()
        val viewModel = createViewModel(
            repository = FakeTaskRepository(initialTasks = listOf(task)),
            taskId = task.id,
        )
        advanceUntilIdle()

        viewModel.onDeleteRequested()

        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun dismissDelete_hidesConfirmationWithoutDeleting() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onDeleteRequested()
        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertTrue(repository.deletedTasks.isEmpty())
    }

    @Test
    fun confirmDelete_deletesOriginalTask() = runTest(testDispatcher) {
        val task = existingTask(isCompleted = true)
        val repository = FakeTaskRepository(initialTasks = listOf(task))
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(task, repository.deletedTasks.single())
    }

    @Test
    fun successfulDelete_signalsNavigation() = runTest(testDispatcher) {
        val task = existingTask()
        val viewModel = createViewModel(
            repository = FakeTaskRepository(initialTasks = listOf(task)),
            taskId = task.id,
        )
        advanceUntilIdle()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.deleteCompleted)
        viewModel.onDeleteCompletedHandled()
        assertFalse(viewModel.uiState.value.deleteCompleted)
    }

    @Test
    fun failedDelete_doesNotSignalNavigationAndAllowsRetry() = runTest(testDispatcher) {
        val task = existingTask()
        val repository = FakeTaskRepository(
            initialTasks = listOf(task),
            deleteFailure = IllegalStateException("Database unavailable"),
        )
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.deleteCompleted)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.deleteError)

        viewModel.onDeleteRequested()
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun repeatedDeleteConfirmation_deletesOnlyOnce() = runTest(testDispatcher) {
        val task = existingTask()
        val deleteGate = CompletableDeferred<Unit>()
        val repository = FakeTaskRepository(
            initialTasks = listOf(task),
            deleteGate = deleteGate,
        )
        val viewModel = createViewModel(repository, task.id)
        advanceUntilIdle()

        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        viewModel.onDeleteConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.isDeleting)
        assertEquals(1, repository.deletedTasks.size)

        deleteGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.deletedTasks.size)
        assertTrue(viewModel.uiState.value.deleteCompleted)
    }

    private fun createViewModel(
        repository: TaskRepository,
        taskId: Long? = null,
        taskIdArgument: String? = taskId?.toString(),
    ) = TaskEditorViewModel(
        taskRepository = repository,
        currentTimeMillis = { timestamp },
        todayEpochDay = todayEpochDay,
        taskIdArgument = taskIdArgument,
    )

    private fun existingTask(
        isCompleted: Boolean = false,
    ) = TaskEntity(
        id = 42,
        title = "Original title",
        description = "Original description",
        priority = TaskPriority.HIGH,
        dueDateEpochDay = todayEpochDay + 2,
        dueTimeMinutes = 10 * 60 + 45,
        isCompleted = isCompleted,
        createdAtEpochMillis = 987_654L,
    )
}

private class FakeTaskRepository(
    initialTasks: List<TaskEntity> = emptyList(),
    private val insertGate: CompletableDeferred<Unit>? = null,
    private val insertFailure: Exception? = null,
    private val updateFailure: Exception? = null,
    private val deleteGate: CompletableDeferred<Unit>? = null,
    private val deleteFailure: Exception? = null,
    private val getFailure: Exception? = null,
) : TaskRepository {
    private val tasks = MutableStateFlow(initialTasks)
    val insertedTasks = mutableListOf<TaskEntity>()
    val updatedTasks = mutableListOf<TaskEntity>()
    val deletedTasks = mutableListOf<TaskEntity>()

    override fun observeTasks(): Flow<List<TaskEntity>> = tasks

    override suspend fun getTask(id: Long): TaskEntity? {
        getFailure?.let { throw it }
        return tasks.value.firstOrNull { task -> task.id == id }
    }

    override suspend fun insertTask(task: TaskEntity): Long {
        insertedTasks += task
        insertGate?.await()
        insertFailure?.let { throw it }
        return 1L
    }

    override suspend fun updateTask(task: TaskEntity) {
        updatedTasks += task
        updateFailure?.let { throw it }
    }

    override suspend fun deleteTask(task: TaskEntity) {
        deletedTasks += task
        deleteGate?.await()
        deleteFailure?.let { throw it }
    }

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) = Unit
}
