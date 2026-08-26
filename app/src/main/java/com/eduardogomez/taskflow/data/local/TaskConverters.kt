package com.eduardogomez.taskflow.data.local

import androidx.room.TypeConverter

class TaskConverters {
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}
