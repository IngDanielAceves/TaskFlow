package com.eduardogomez.taskflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFlowNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addTaskButton_opensCreateEditor() {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_task))
            .performClick()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.new_task_title))
            .assertIsDisplayed()
    }

    @Test
    fun persistedTask_opensEditAndDeleteDialogCanBeCancelled() {
        val taskTitle = "Navigation test ${System.nanoTime()}"
        createTask(taskTitle)

        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                nodeExists(taskTitle)
            }
            composeRule.onNodeWithText(taskTitle).performScrollTo().performClick()

            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.edit_task_title))
                .assertIsDisplayed()
            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.delete_task))
                .performScrollTo()
                .performClick()
            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.delete_task_dialog_title))
                .assertIsDisplayed()

            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.action_cancel))
                .performClick()

            composeRule
                .onNodeWithText(composeRule.activity.getString(R.string.edit_task_title))
                .assertIsDisplayed()
            composeRule.onNodeWithText(taskTitle).assertIsDisplayed()
        } finally {
            deleteTaskIfPresent(taskTitle)
        }
    }

    private fun createTask(title: String) {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_task))
            .performClick()
        composeRule
            .onNode(
                hasText(composeRule.activity.getString(R.string.task_title_label)) and
                    hasSetTextAction(),
            )
            .performTextInput(title)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.create_task))
            .performScrollTo()
            .performClick()
    }

    private fun deleteTaskIfPresent(title: String) {
        val deleteTask = composeRule.activity.getString(R.string.delete_task)
        val deleteDialogTitle = composeRule.activity.getString(R.string.delete_task_dialog_title)

        when {
            nodeExists(deleteDialogTitle) -> Unit
            nodeExists(deleteTask) -> composeRule
                .onNodeWithText(deleteTask)
                .performScrollTo()
                .performClick()
            nodeExists(title) -> {
                composeRule.onNodeWithText(title).performScrollTo().performClick()
                composeRule.waitUntil(timeoutMillis = 5_000) {
                    nodeExists(deleteTask)
                }
                composeRule
                    .onNodeWithText(deleteTask)
                    .performScrollTo()
                    .performClick()
            }
            else -> return
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.action_delete))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !nodeExists(title)
        }
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_task))
            .assertIsDisplayed()
    }

    private fun nodeExists(text: String): Boolean =
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
}
