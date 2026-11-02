package com.demo.taskmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demo.taskmanager.R
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.ui.theme.MinTouchTarget
import com.demo.taskmanager.ui.theme.Spacing
import com.demo.taskmanager.ui.theme.extraColors

@Composable
fun TaskRow(
    task: Task,
    onEdit: () -> Unit,
    onChangeStatus: (Status) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (task.overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val description = stringResource(
        if (task.overdue) R.string.row_description_overdue else R.string.row_description,
        task.title,
        task.priority.label,
        task.status.label,
        task.dueDate,
    )
    Column(modifier = modifier.background(background).testTag("task_row_${task.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
                    .heightIn(min = MinTouchTarget)
                    .semantics(mergeDescendants = true) { contentDescription = description },
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaLine(task, onChangeStatus)
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_${task.id}")) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_task))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun MetaLine(task: Task, onChangeStatus: (Status) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = task.priority.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (task.priority == Priority.LOW) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (task.priority == Priority.HIGH) FontWeight.Bold else null,
        )
        StatusMenu(task, onChangeStatus)
        Text(
            text = task.dueDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (task.overdue) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.overdue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StatusMenu(task: Task, onChangeStatus: (Status) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .testTag("status_${task.id}"),
        ) {
            Text(
                text = task.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = task.status.color(),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Status.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    leadingIcon = if (status == task.status) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onChangeStatus(status)
                    },
                    modifier = Modifier.testTag("status_option_${status.label}"),
                )
            }
        }
    }
}

@Composable
private fun Status.color(): Color = when (this) {
    Status.TODO -> MaterialTheme.extraColors.info
    Status.IN_PROGRESS -> MaterialTheme.extraColors.warning
    Status.DONE -> MaterialTheme.extraColors.success
}
