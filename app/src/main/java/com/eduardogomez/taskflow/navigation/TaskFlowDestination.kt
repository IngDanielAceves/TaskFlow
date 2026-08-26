package com.eduardogomez.taskflow.navigation

enum class TaskFlowDestination(val route: String) {
    HOME("home"),
    TASK_EDITOR("task_editor"),
    ;

    companion object {
        const val TASK_ID_ARGUMENT = "taskId"
        val taskEditorRoutePattern =
            "${TASK_EDITOR.route}?$TASK_ID_ARGUMENT={$TASK_ID_ARGUMENT}"

        fun taskEditorRoute(taskId: Long? = null): String =
            if (taskId == null) {
                TASK_EDITOR.route
            } else {
                "${TASK_EDITOR.route}?$TASK_ID_ARGUMENT=$taskId"
            }
    }
}
