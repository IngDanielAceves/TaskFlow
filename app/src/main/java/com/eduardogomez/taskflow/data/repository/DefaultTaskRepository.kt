package com.eduardogomez.taskflow.data.repository

import com.eduardogomez.taskflow.data.local.TaskDao
import com.eduardogomez.taskflow.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

class DefaultTaskRepository(
    private val taskDao: TaskDao,
) : TaskRepository {
    override fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeAll()

    override suspend fun getTask(id: Long): TaskEntity? = taskDao.getById(id)

    override suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)

    override suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    override suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    override suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) =
        taskDao.updateCompletion(id, isCompleted)
}
