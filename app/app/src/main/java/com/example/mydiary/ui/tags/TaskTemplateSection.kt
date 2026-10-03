package com.example.mydiary.ui.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.TaskItem
import com.example.mydiary.data.TaskRepository
import kotlinx.coroutines.launch

/**
 * 任务模板编辑区：创建周/月模板 + 添加任务项。
 */
@Composable
fun TaskTemplateSection(
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val templates by taskRepository.observeTemplates().collectAsState(initial = emptyList())
    var newTemplateName by remember { mutableStateOf("") }
    var newItemTitle by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("任务模板", style = MaterialTheme.typography.titleMedium)

        // 创建模板
        Row {
            TextField(
                value = newTemplateName,
                onValueChange = { newTemplateName = it },
                modifier = Modifier.weight(1f),
                label = { Text("模板名称") },
            )
            Spacer(modifier = Modifier.padding(8.dp))
            OutlinedButton(
                onClick = {
                    if (newTemplateName.isNotBlank()) {
                        scope.launch {
                            val id = taskRepository.createTemplate("weekly", newTemplateName)
                            selectedTemplateId = id.toInt()
                            newTemplateName = ""
                        }
                    }
                },
            ) { Text("创建周模板") }
        }

        // 已有模板列表
        templates.forEach { template ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "${template.name} (${if (template.period == "weekly") "周" else "月"})",
                    modifier = Modifier.weight(1f),
                )
                if (selectedTemplateId == template.id) {
                    Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }

            // 选中后显示任务项管理
            if (selectedTemplateId == template.id) {
                val items by taskRepository.observeItems(template.id).collectAsState(initial = emptyList())
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    items.forEach { item ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(item.title, modifier = Modifier.weight(1f))
                            Text(
                                "删除",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            // 简化：点文字删除（后续改按钮）
                        }
                    }
                    Row {
                        TextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("新任务项") },
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Button(
                            onClick = {
                                if (newItemTitle.isNotBlank()) {
                                    scope.launch {
                                        taskRepository.addTaskItem(template.id, newItemTitle)
                                        newItemTitle = ""
                                    }
                                }
                            },
                        ) { Text("添加") }
                    }
                }
            }
        }
    }
}
