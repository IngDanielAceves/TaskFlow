package com.eduardogomez.taskflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eduardogomez.taskflow.feature.home.HomeRoute
import com.eduardogomez.taskflow.feature.taskeditor.TaskEditorRoute

@Composable
fun TaskFlowNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = TaskFlowDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(route = TaskFlowDestination.HOME.route) {
            HomeRoute(
                onOpenTaskEditor = {
                    navController.navigate(TaskFlowDestination.TASK_EDITOR.route)
                },
            )
        }

        composable(route = TaskFlowDestination.TASK_EDITOR.route) {
            TaskEditorRoute(onNavigateBack = navController::navigateUp)
        }
    }
}
