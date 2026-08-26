package com.eduardogomez.taskflow.di

import com.eduardogomez.taskflow.data.repository.DefaultTaskRepository
import com.eduardogomez.taskflow.data.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTaskRepository(
        defaultTaskRepository: DefaultTaskRepository,
    ): TaskRepository
}
