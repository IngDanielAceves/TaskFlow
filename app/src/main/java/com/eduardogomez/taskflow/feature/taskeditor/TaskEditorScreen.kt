package com.eduardogomez.taskflow.feature.taskeditor

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduardogomez.taskflow.R
import com.eduardogomez.taskflow.data.local.TaskPriority
import com.eduardogomez.taskflow.ui.theme.PriorityHighContainer
import com.eduardogomez.taskflow.ui.theme.PriorityHighContainerDark
import com.eduardogomez.taskflow.ui.theme.PriorityHighContent
import com.eduardogomez.taskflow.ui.theme.PriorityHighContentDark
import com.eduardogomez.taskflow.ui.theme.PriorityLowContainer
import com.eduardogomez.taskflow.ui.theme.PriorityLowContainerDark
import com.eduardogomez.taskflow.ui.theme.PriorityLowContent
import com.eduardogomez.taskflow.ui.theme.PriorityLowContentDark
import com.eduardogomez.taskflow.ui.theme.PriorityMediumContainer
import com.eduardogomez.taskflow.ui.theme.PriorityMediumContainerDark
import com.eduardogomez.taskflow.ui.theme.PriorityMediumContent
import com.eduardogomez.taskflow.ui.theme.PriorityMediumContentDark
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TaskEditorRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onNavigateBack()
        }
    }

    TaskEditorScreen(
        uiState = uiState,
        onTitleChanged = viewModel::onTitleChanged,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onPriorityChanged = viewModel::onPriorityChanged,
        onDueDateChanged = viewModel::onDueDateChanged,
        onDueTimeChanged = viewModel::onDueTimeChanged,
        onCreateTask = viewModel::saveTask,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    uiState: TaskEditorUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPriorityChanged: (TaskPriority) -> Unit,
    onDueDateChanged: (Long) -> Unit,
    onDueTimeChanged: (Int) -> Unit,
    onCreateTask: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val descriptionFocusRequester = remember { FocusRequester() }
    val userScrollConnection = remember(focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }
    val focusClearingSpacerModifier = Modifier
        .fillMaxWidth()
        .height(24.dp)
        .pointerInput(focusManager) {
            detectTapGestures { focusManager.clearFocus() }
        }
    val dueDateText = formatDueDate(uiState.dueDateEpochDay)
    val dueTimeText = formatDueTime(uiState.dueTimeMinutes)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.mode == TaskEditorMode.CREATE) {
                                R.string.new_task_title
                            } else {
                                R.string.edit_task_title
                            },
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.loadError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.task_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .nestedScroll(userScrollConnection)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.task_title_label)) },
                placeholder = { Text(text = stringResource(R.string.task_title_placeholder)) },
                supportingText = if (uiState.titleError) {
                    { Text(text = stringResource(R.string.title_required)) }
                } else {
                    null
                },
                isError = uiState.titleError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { descriptionFocusRequester.requestFocus() },
                ),
            )

            Spacer(modifier = focusClearingSpacerModifier)

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester),
                label = { Text(text = stringResource(R.string.description_label)) },
                placeholder = { Text(text = stringResource(R.string.description_placeholder)) },
                minLines = 3,
                maxLines = 5,
            )

            Spacer(modifier = focusClearingSpacerModifier)

            PrioritySelector(
                selectedPriority = uiState.priority,
                onPrioritySelected = {
                    focusManager.clearFocus()
                    onPriorityChanged(it)
                },
            )

            Spacer(modifier = focusClearingSpacerModifier)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionRow(
                    iconResId = R.drawable.ic_calendar_24,
                    labelResId = R.string.due_date_label,
                    value = dueDateText,
                    onClick = {
                        focusManager.clearFocus()
                        showDatePicker = true
                    },
                )
                SelectionRow(
                    iconResId = R.drawable.ic_clock_24,
                    labelResId = R.string.due_time_label,
                    value = dueTimeText,
                    onClick = {
                        focusManager.clearFocus()
                        showTimePicker = true
                    },
                )
            }

            Spacer(modifier = focusClearingSpacerModifier)

            Button(
                onClick = {
                    if (uiState.title.isNotBlank()) {
                        focusManager.clearFocus()
                    }
                    onCreateTask()
                },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    text = stringResource(
                        if (uiState.mode == TaskEditorMode.CREATE) {
                            R.string.create_task
                        } else {
                            R.string.save_changes
                        },
                    ),
                )
            }

            if (uiState.saveError) {
                Text(
                    text = stringResource(R.string.task_save_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        }
    }

    if (showDatePicker) {
        TaskDatePickerDialog(
            selectedDateEpochDay = uiState.dueDateEpochDay,
            onDateSelected = onDueDateChanged,
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        TaskTimePickerDialog(
            initialHour = uiState.dueTimeMinutes?.div(MINUTES_PER_HOUR),
            initialMinute = uiState.dueTimeMinutes?.rem(MINUTES_PER_HOUR),
            onTimeSelected = { hour, minute ->
                onDueTimeChanged(hour * MINUTES_PER_HOUR + minute)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.priority_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskPriority.entries.forEach { priority ->
                val selectedColors = priority.selectedColors(isDarkTheme)
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(text = stringResource(priority.labelResId())) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColors.container,
                        selectedLabelColor = selectedColors.content,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SelectionRow(
    @DrawableRes iconResId: Int,
    @StringRes labelResId: Int,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePickerDialog(
    selectedDateEpochDay: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayEpochDay = LocalDate.now().toEpochDay()
    val selectableDates = remember(todayEpochDay) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toUtcEpochDay() >= todayEpochDay

            override fun isSelectableYear(year: Int): Boolean =
                year >= LocalDate.ofEpochDay(todayEpochDay).year
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate
            .ofEpochDay(maxOf(selectedDateEpochDay, todayEpochDay))
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                        val selectedDateEpochDay = selectedDateMillis.toUtcEpochDay()
                        if (selectedDateEpochDay >= todayEpochDay) {
                            onDateSelected(selectedDateEpochDay)
                        }
                    }
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTimePickerDialog(
    initialHour: Int?,
    initialMinute: Int?,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentTime = remember { LocalTime.now() }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour ?: currentTime.hour,
        initialMinute = initialMinute ?: currentTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        },
    )
}

@Composable
private fun formatDueDate(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return if (date == LocalDate.now()) {
        stringResource(R.string.due_date_today)
    } else {
        remember(date) {
            date.format(
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault()),
            )
        }
    }
}

@Composable
private fun formatDueTime(dueTimeMinutes: Int?): String {
    if (dueTimeMinutes == null) {
        return stringResource(R.string.due_time_not_set)
    }

    return remember(dueTimeMinutes) {
        LocalTime.of(
            dueTimeMinutes / MINUTES_PER_HOUR,
            dueTimeMinutes % MINUTES_PER_HOUR,
        ).format(
            DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault()),
        )
    }
}

private fun Long.toUtcEpochDay(): Long = Instant
    .ofEpochMilli(this)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .toEpochDay()

private data class SelectedPriorityColors(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
)

@StringRes
private fun TaskPriority.labelResId(): Int = when (this) {
    TaskPriority.LOW -> R.string.priority_low
    TaskPriority.MEDIUM -> R.string.priority_medium
    TaskPriority.HIGH -> R.string.priority_high
}

private fun TaskPriority.selectedColors(isDarkTheme: Boolean): SelectedPriorityColors =
    when (this) {
        TaskPriority.LOW -> SelectedPriorityColors(
            container = if (isDarkTheme) PriorityLowContainerDark else PriorityLowContainer,
            content = if (isDarkTheme) PriorityLowContentDark else PriorityLowContent,
        )

        TaskPriority.MEDIUM -> SelectedPriorityColors(
            container = if (isDarkTheme) PriorityMediumContainerDark else PriorityMediumContainer,
            content = if (isDarkTheme) PriorityMediumContentDark else PriorityMediumContent,
        )

        TaskPriority.HIGH -> SelectedPriorityColors(
            container = if (isDarkTheme) PriorityHighContainerDark else PriorityHighContainer,
            content = if (isDarkTheme) PriorityHighContentDark else PriorityHighContent,
        )
    }

private const val MINUTES_PER_HOUR = 60
