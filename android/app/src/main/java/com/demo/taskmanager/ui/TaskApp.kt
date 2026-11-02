package com.demo.taskmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.taskmanager.R
import com.demo.taskmanager.ui.theme.MaxContentWidth

@Composable
fun TaskApp(viewModel: TaskListViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.textRes()) }

    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbarHost.showSnackbar(messageText, duration = SnackbarDuration.Short)
            viewModel.consumeMessage()
        }
    }

    BackHandler(enabled = state.form != null, onBack = viewModel::closeForm)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val form = state.form
        Box(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
            if (form == null) {
                TaskListScreen(
                    state = state,
                    snackbarHost = snackbarHost,
                    actions = TaskListActions(
                        onRetry = viewModel::retry,
                        onStatusFilter = viewModel::setStatusFilter,
                        onPriorityFilter = viewModel::setPriorityFilter,
                        onClearFilters = viewModel::clearFilters,
                        onAdd = viewModel::openCreate,
                        onEdit = viewModel::openEdit,
                        onChangeStatus = viewModel::changeStatus,
                        onAskDelete = viewModel::askDelete,
                        onCancelDelete = viewModel::cancelDelete,
                        onConfirmDelete = viewModel::confirmDelete,
                    ),
                )
            } else {
                TaskFormScreen(
                    form = form,
                    snackbarHost = snackbarHost,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::save,
                    onClose = viewModel::closeForm,
                )
            }
        }
    }
}

private fun UiMessage.textRes(): Int = when (this) {
    UiMessage.TaskCreated -> R.string.message_created
    UiMessage.TaskUpdated -> R.string.message_updated
    UiMessage.TaskDeleted -> R.string.message_deleted
    UiMessage.TaskGone -> R.string.message_gone
    UiMessage.NetworkError -> R.string.message_network
    UiMessage.ServerError -> R.string.message_server
}
