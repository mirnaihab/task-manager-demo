package com.demo.taskmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.demo.taskmanager.R
import com.demo.taskmanager.data.Priority
import com.demo.taskmanager.data.Status
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.ui.theme.Spacing

class TaskListActions(
    val onRetry: () -> Unit,
    val onStatusFilter: (Status?) -> Unit,
    val onPriorityFilter: (Priority?) -> Unit,
    val onClearFilters: () -> Unit,
    val onAdd: () -> Unit,
    val onEdit: (Task) -> Unit,
    val onChangeStatus: (Task, Status) -> Unit,
    val onAskDelete: (Task) -> Unit,
    val onCancelDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(state: UiState, snackbarHost: SnackbarHostState, actions: TaskListActions) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title), style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onAdd,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                Phase.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).testTag("loading"),
                )
                Phase.Error -> ErrorState(actions.onRetry)
                Phase.Content -> TaskList(state, actions)
            }
            if (state.isRefreshing && state.phase == Phase.Content) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    state.pendingDelete?.let { DeleteDialog(it, actions.onCancelDelete, actions.onConfirmDelete) }
}

@Composable
private fun TaskList(state: UiState, actions: TaskListActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("task_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SummaryPanel(state.summary)
                FilterRow(
                    options = Status.entries,
                    selected = state.filters.status,
                    label = { it.label },
                    onSelect = actions.onStatusFilter,
                    tagPrefix = "status_chip",
                )
                FilterRow(
                    options = Priority.entries,
                    selected = state.filters.priority,
                    label = { it.label },
                    onSelect = actions.onPriorityFilter,
                    tagPrefix = "priority_chip",
                )
            }
        }
        if (state.tasks.isEmpty()) {
            item { EmptyState(state.filters.isActive, actions.onClearFilters) }
        }
        items(state.tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                onEdit = { actions.onEdit(task) },
                onChangeStatus = { actions.onChangeStatus(task, it) },
                onDelete = { actions.onAskDelete(task) },
            )
        }
    }
}

@Composable
private fun EmptyState(filtersActive: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.lg).testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(if (filtersActive) R.string.empty_filtered else R.string.empty_no_tasks),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (filtersActive) {
            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.clear_filters)) }
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg).testTag("error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.error_network),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = Spacing.md)) {
            Text(stringResource(R.string.try_again))
        }
    }
}
