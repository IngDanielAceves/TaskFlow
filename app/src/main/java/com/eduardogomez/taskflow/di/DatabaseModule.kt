package com.eduardogomez.taskflow.di

import android.content.Context
import androidx.room.Room
import com.eduardogomez.taskflow.data.local.TaskDao
import com.eduardogomez.taskflow.data.local.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideTaskDatabase(
        @ApplicationContext context: Context,
    ): TaskDatabase =
        Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "taskflow.db",
        ).build()

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()
}
