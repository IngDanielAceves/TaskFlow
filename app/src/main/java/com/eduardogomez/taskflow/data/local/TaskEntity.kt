package com.eduardogomez.taskflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val priority: TaskPriority,
    val dueDateEpochDay: Long? = null,
    val dueTimeMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long,
)
