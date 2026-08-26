package com.eduardogomez.taskflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                onCreateTask = {
                    navController.navigate(TaskFlowDestination.taskEditorRoute())
                },
                onOpenTask = { taskId ->
                    navController.navigate(TaskFlowDestination.taskEditorRoute(taskId))
                },
            )
        }

        composable(
            route = TaskFlowDestination.taskEditorRoutePattern,
            arguments = listOf(
                navArgument(TaskFlowDestination.TASK_ID_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            TaskEditorRoute(onNavigateBack = navController::navigateUp)
        }
    }
}
