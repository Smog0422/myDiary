package com.example.mydiary.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.TaskInstance
import com.example.mydiary.data.TaskRepository

/**
 * 任务清单区块：显示某周期的任务实例，可逐项勾选。
 */
@Composable
fun TaskListSection(
    title: String,
    instances: List<TaskInstance>,
    onToggle: (TaskInstance) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (instances.isEmpty()) {
            Text("暂无任务", style = MaterialTheme.typography.bodyMedium)
        }

        instances.forEach { instance ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = instance.done,
                    onCheckedChange = { onToggle(instance) },
                )
                Text(
                    text = instance.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .let { 
                            if (instance.done) it else it 
                        },
                )
            }
        }
    }
}
