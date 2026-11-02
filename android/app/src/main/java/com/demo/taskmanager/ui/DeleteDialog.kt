package com.demo.taskmanager.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.demo.taskmanager.R
import com.demo.taskmanager.data.Task
import com.demo.taskmanager.ui.theme.Radius
import androidx.compose.runtime.remember

@Composable
fun DeleteDialog(task: Task, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }

    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(Radius.lg),
        title = { Text(stringResource(R.string.delete_dialog_title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                stringResource(R.string.delete_dialog_body, task.title),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.focusRequester(cancelFocus).testTag("delete_cancel"),
            ) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.secondary)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag("delete_confirm"),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
    )
}
