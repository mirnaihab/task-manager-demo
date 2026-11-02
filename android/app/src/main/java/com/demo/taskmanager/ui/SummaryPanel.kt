package com.demo.taskmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.demo.taskmanager.R
import com.demo.taskmanager.data.Summary
import com.demo.taskmanager.ui.theme.Radius
import com.demo.taskmanager.ui.theme.Spacing

@Composable
fun SummaryPanel(summary: Summary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Radius.lg))
            .padding(Spacing.md)
            .testTag("summary_panel"),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Count(
            value = summary.total,
            caption = stringResource(R.string.summary_total),
            big = true,
            tag = "count_total",
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Count(summary.todo, stringResource(R.string.summary_todo), tag = "count_todo")
            Count(summary.inProgress, stringResource(R.string.summary_in_progress), tag = "count_in_progress")
            Count(summary.done, stringResource(R.string.summary_done), tag = "count_done")
        }
        val overdueColor =
            if (summary.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        Text(
            text = stringResource(R.string.summary_overdue, summary.overdue),
            style = MaterialTheme.typography.labelSmall,
            color = overdueColor,
            modifier = Modifier.testTag("count_overdue"),
        )
    }
}

@Composable
private fun Count(value: Int, caption: String, tag: String, big: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(tag)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = value.toString(),
            style = if (big) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
