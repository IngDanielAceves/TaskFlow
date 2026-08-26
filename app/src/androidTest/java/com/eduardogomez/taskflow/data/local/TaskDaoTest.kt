package com.eduardogomez.taskflow.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TaskDatabase::class.java,
        ).build()
        taskDao = database.taskDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insert_emitsTaskFromObservedList() = runBlocking {
        val task = createTask(title = "Learn Room")

        val id = taskDao.insert(task)

        assertEquals(listOf(task.copy(id = id)), taskDao.observeAll().first())
    }

    @Test
    fun getById_returnsMatchingTask() = runBlocking {
        val task = createTask(title = "Read DAO documentation")
        val id = taskDao.insert(task)

        val storedTask = taskDao.getById(id)

        assertEquals(task.copy(id = id), storedTask)
    }

    @Test
    fun update_replacesStoredTask() = runBlocking {
        val task = createTask(title = "Original title")
        val id = taskDao.insert(task)
        val updatedTask = task.copy(
            id = id,
            title = "Updated title",
            description = "Updated description",
            priority = TaskPriority.HIGH,
        )

        taskDao.update(updatedTask)

        assertEquals(updatedTask, taskDao.getById(id))
    }

    @Test
    fun updateCompletion_changesOnlyCompletionState() = runBlocking {
        val task = createTask(title = "Complete Room tests")
        val id = taskDao.insert(task)

        taskDao.updateCompletion(id = id, isCompleted = true)

        val completedTask = taskDao.getById(id)
        assertTrue(completedTask?.isCompleted == true)
        assertEquals(task.copy(id = id, isCompleted = true), completedTask)

        taskDao.updateCompletion(id = id, isCompleted = false)

        assertFalse(taskDao.getById(id)?.isCompleted ?: true)
    }

    @Test
    fun delete_removesTask() = runBlocking {
        val task = createTask(title = "Temporary task")
        val id = taskDao.insert(task)

        taskDao.delete(task.copy(id = id))

        assertNull(taskDao.getById(id))
    }

    @Test
    fun observeAll_ordersTasksDeterministically() = runBlocking {
        val pendingSameDateLaterCreated = insertTask(
            title = "Pending same date, later creation",
            dueDateEpochDay = 100,
            createdAtEpochMillis = 20,
        )
        val pendingSameDateFirstId = insertTask(
            title = "Pending same date and creation, first id",
            dueDateEpochDay = 100,
            createdAtEpochMillis = 10,
        )
        val pendingSameDateSecondId = insertTask(
            title = "Pending same date and creation, second id",
            dueDateEpochDay = 100,
            createdAtEpochMillis = 10,
        )
        val pendingLaterDate = insertTask(
            title = "Pending later date",
            dueDateEpochDay = 200,
            createdAtEpochMillis = 1,
        )
        val pendingWithoutDate = insertTask(
            title = "Pending without date",
            dueDateEpochDay = null,
            createdAtEpochMillis = 1,
        )
        val completedWithDate = insertTask(
            title = "Completed with date",
            dueDateEpochDay = 50,
            createdAtEpochMillis = 1,
            isCompleted = true,
        )
        val completedWithoutDate = insertTask(
            title = "Completed without date",
            dueDateEpochDay = null,
            createdAtEpochMillis = 1,
            isCompleted = true,
        )

        val orderedIds = taskDao.observeAll().first().map(TaskEntity::id)

        assertEquals(
            listOf(
                pendingSameDateFirstId,
                pendingSameDateSecondId,
                pendingSameDateLaterCreated,
                pendingLaterDate,
                pendingWithoutDate,
                completedWithDate,
                completedWithoutDate,
            ),
            orderedIds,
        )
    }

    private suspend fun insertTask(
        title: String,
        dueDateEpochDay: Long?,
        createdAtEpochMillis: Long,
        isCompleted: Boolean = false,
    ): Long = taskDao.insert(
        createTask(
            title = title,
            dueDateEpochDay = dueDateEpochDay,
            createdAtEpochMillis = createdAtEpochMillis,
            isCompleted = isCompleted,
        ),
    )

    private fun createTask(
        title: String,
        description: String? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDateEpochDay: Long? = null,
        dueTimeMinutes: Int? = null,
        isCompleted: Boolean = false,
        createdAtEpochMillis: Long = 1,
    ) = TaskEntity(
        title = title,
        description = description,
        priority = priority,
        dueDateEpochDay = dueDateEpochDay,
        dueTimeMinutes = dueTimeMinutes,
        isCompleted = isCompleted,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
