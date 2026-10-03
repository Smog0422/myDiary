package com.example.mydiary.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.CheckInRepository
import com.example.mydiary.data.Tag
import com.example.mydiary.data.TaskInstance
import com.example.mydiary.data.TaskRepository
import com.example.mydiary.data.currentPeriodKeys
import kotlinx.coroutines.launch

/**
 * 首页：任务清单区（本周+本月）+ 快捷打卡区。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tagRepository: com.example.mydiary.data.TagRepository,
    checkInRepository: CheckInRepository,
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tags by tagRepository.observeAll().collectAsState(initial = emptyList())

    // 当前周期标识
    val (weekKey, monthKey) = remember { currentPeriodKeys() }
    val weeklyTasks by taskRepository.observeInstancesByPeriod(weekKey).collectAsState(initial = emptyList())
    val monthlyTasks by taskRepository.observeInstancesByPeriod(monthKey).collectAsState(initial = emptyList())

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteTargetTag by remember { mutableStateOf<Tag?>(null) }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("MyDiary", style = MaterialTheme.typography.headlineSmall)

            // 本周任务
            TaskListSection(
                title = "本周任务",
                instances = weeklyTasks,
                onToggle = { inst -> scope.launch { taskRepository.toggleDone(inst) } },
            )

            // 本月任务
            TaskListSection(
                title = "本月任务",
                instances = monthlyTasks,
                onToggle = { inst -> scope.launch { taskRepository.toggleDone(inst) } },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 快捷打卡区
            Text("记一笔", style = MaterialTheme.typography.titleMedium)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tags.isEmpty()) {
                    Text("还没有标签，去管理页创建一个吧", style = MaterialTheme.typography.bodyMedium)
                }
                tags.forEach { tag ->
                    CheckInButton(
                        tag = tag,
                        onClick = {
                            noteTargetTag = tag
                            showNoteDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteDialog(
            tag = noteTargetTag!!,
            onConfirm = { note ->
                scope.launch { checkInRepository.record(noteTargetTag!!.id, note) }
                showNoteDialog = false
            },
            onQuickRecord = {
                scope.launch { checkInRepository.record(noteTargetTag!!.id, null) }
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false },
        )
    }
}

@Composable
private fun CheckInButton(tag: Tag, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(tag.color).copy(alpha = 0.2f),
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(tag.color)),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NoteDialog(
    tag: Tag,
    onConfirm: (String?) -> Unit,
    onQuickRecord: () -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记一笔：${tag.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("加一句备注？（可选）") },
                    minLines = 1,
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onQuickRecord) { Text("直接记录") }
                Button(onClick = { onConfirm(note.takeIf { it.isNotBlank() }) }) { Text("记录") }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
