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

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(taskTitle)).fetchSemanticsNodes().isNotEmpty()
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

        deleteTaskAndWaitForHome(taskTitle)
    }

    private fun createTask(title: String) {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_task))
            .performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(title)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.create_task))
            .performScrollTo()
            .performClick()
    }

    private fun deleteTaskAndWaitForHome(title: String) {
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.delete_task))
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.action_delete))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(title)).fetchSemanticsNodes().isEmpty()
        }
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.add_task))
            .assertIsDisplayed()
    }
}
