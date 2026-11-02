package com.demo.taskmanager.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demo.taskmanager.R
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.TaskDraft
import com.demo.taskmanager.ui.theme.MinTouchTarget
import com.demo.taskmanager.ui.theme.Radius
import com.demo.taskmanager.ui.theme.Spacing
import com.demo.taskmanager.ui.theme.extraColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    form: FormState,
    snackbarHost: SnackbarHostState,
    onDraftChange: (TaskDraft) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val draft = form.draft
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (form.editingId == null) R.string.form_new_title else R.string.form_edit_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.form_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val titleMessage = when {
                form.titleError == TitleError.Required -> stringResource(R.string.title_required)
                form.titleError == TitleError.TooLong -> stringResource(R.string.title_too_long)
                else -> form.serverErrors["title"]
            }
            FormField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                label = stringResource(R.string.field_title),
                error = titleMessage,
                counter = stringResource(R.string.title_counter, draft.title.length, TITLE_MAX_LENGTH),
                modifier = Modifier.testTag("field_title"),
            )
            FormField(
                value = draft.description,
                onValueChange = { onDraftChange(draft.copy(description = it)) },
                label = stringResource(R.string.field_description),
                error = form.serverErrors["description"],
                minLines = 3,
                modifier = Modifier.testTag("field_description"),
            )
            PriorityPicker(draft.priority, form.serverErrors["priority"]) { onDraftChange(draft.copy(priority = it)) }
            DueDateField(draft.dueDate, form.serverErrors["dueDate"]) { onDraftChange(draft.copy(dueDate = it)) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
            ) {
                TextButton(onClick = onClose, modifier = Modifier.heightIn(min = MinTouchTarget).testTag("form_cancel")) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.secondary)
                }
                Button(
                    onClick = onSave,
                    enabled = !form.isSaving,
                    shape = RoundedCornerShape(Radius.md),
                    modifier = Modifier.heightIn(min = MinTouchTarget).testTag("form_save"),
                ) {
                    if (form.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    counter: String? = null,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            if (error != null || counter != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(error.orEmpty(), modifier = Modifier.weight(1f))
                    if (counter != null) Text(counter)
                }
            }
        },
        minLines = minLines,
        shape = RoundedCornerShape(Radius.md),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedBorderColor = MaterialTheme.extraColors.outlineStrong,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun PriorityPicker(selected: Priority, error: String?, onSelect: (Priority) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(R.string.field_priority),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Priority.entries.forEachIndexed { index, priority ->
                SegmentedButton(
                    selected = priority == selected,
                    onClick = { onSelect(priority) },
                    shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        inactiveBorderColor = MaterialTheme.extraColors.outlineStrong,
                    ),
                    modifier = Modifier.heightIn(min = MinTouchTarget).testTag("priority_${priority.label}"),
                ) {
                    Text(priority.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateField(dueDate: String, error: String?, onSelect: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (pressed) {
        showPicker = true
    }

    OutlinedTextField(
        value = dueDate,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.field_due_date)) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        interactionSource = interaction,
        shape = RoundedCornerShape(Radius.md),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth().testTag("field_due_date"),
    )

    if (showPicker) {
        val today = remember { LocalDate.now() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(dueDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isSelectableDay(utcTimeMillis, today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onSelect(utcMillisToIsoDate(it)) }
                        showPicker = false
                    },
                    modifier = Modifier.testTag("date_ok"),
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
