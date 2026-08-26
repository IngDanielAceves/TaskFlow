package com.eduardogomez.taskflow.data.repository

import com.eduardogomez.taskflow.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<TaskEntity>>

    suspend fun getTask(id: Long): TaskEntity?

    suspend fun insertTask(task: TaskEntity): Long

    suspend fun updateTask(task: TaskEntity)

    suspend fun deleteTask(task: TaskEntity)

    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean)
}
