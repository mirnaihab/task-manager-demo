package com.demo.taskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.demo.taskmanager.ui.TaskApp
import com.demo.taskmanager.ui.TaskListViewModel
import com.demo.taskmanager.ui.theme.TaskManagerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TaskListViewModel by viewModels {
        TaskListViewModel.factory(AppContainer().repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskManagerTheme {
                TaskApp(viewModel)
            }
        }
    }
}
