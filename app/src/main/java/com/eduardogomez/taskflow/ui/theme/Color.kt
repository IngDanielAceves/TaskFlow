package com.eduardogomez.taskflow.ui.theme

import androidx.compose.ui.graphics.Color
import com.eduardogomez.taskflow.data.local.TaskPriority

val IndigoPrimary = Color(0xFF4F46E5)
val IndigoOnPrimary = Color(0xFFFFFFFF)
val IndigoPrimaryContainer = Color(0xFFE0E7FF)
val IndigoOnPrimaryContainer = Color(0xFF1E1B4B)

val IndigoPrimaryDark = Color(0xFFC7D2FE)
val IndigoOnPrimaryDark = Color(0xFF29216E)
val IndigoPrimaryContainerDark = Color(0xFF3730A3)
val IndigoOnPrimaryContainerDark = Color(0xFFE0E7FF)

val LightBackground = Color(0xFFF8F9FF)
val LightOnBackground = Color(0xFF1B1B1F)
val DarkBackground = Color(0xFF121318)
val DarkOnBackground = Color(0xFFE4E1E9)

val PriorityLowContainer = Color(0xFFDBEAFE)
val PriorityLowContent = Color(0xFF1D4ED8)
val PriorityMediumContainer = Color(0xFFFEF3C7)
val PriorityMediumContent = Color(0xFF92400E)
val PriorityHighContainer = Color(0xFFFEE2E2)
val PriorityHighContent = Color(0xFFB91C1C)

val PriorityLowContainerDark = Color(0xFF1E3A5F)
val PriorityLowContentDark = Color(0xFFBFDBFE)
val PriorityMediumContainerDark = Color(0xFF4A3515)
val PriorityMediumContentDark = Color(0xFFFDE68A)
val PriorityHighContainerDark = Color(0xFF512428)
val PriorityHighContentDark = Color(0xFFFECACA)

internal data class TaskPriorityColors(
    val container: Color,
    val content: Color,
)

internal fun TaskPriority.priorityColors(isDarkTheme: Boolean): TaskPriorityColors =
    when (this) {
        TaskPriority.LOW -> TaskPriorityColors(
            container = if (isDarkTheme) PriorityLowContainerDark else PriorityLowContainer,
            content = if (isDarkTheme) PriorityLowContentDark else PriorityLowContent,
        )

        TaskPriority.MEDIUM -> TaskPriorityColors(
            container = if (isDarkTheme) PriorityMediumContainerDark else PriorityMediumContainer,
            content = if (isDarkTheme) PriorityMediumContentDark else PriorityMediumContent,
        )

        TaskPriority.HIGH -> TaskPriorityColors(
            container = if (isDarkTheme) PriorityHighContainerDark else PriorityHighContainer,
            content = if (isDarkTheme) PriorityHighContentDark else PriorityHighContent,
        )
    }
