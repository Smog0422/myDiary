package com.example.mydiary.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.mydiary.data.PeriodSyncService
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
    syncService: PeriodSyncService,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tags by tagRepository.observeAll().collectAsState(initial = emptyList())

    // 当前周期标识（刷新后重新计算，跨周/跨月时自动切换）
    var periodKeys by remember { mutableStateOf(currentPeriodKeys()) }
    val (weekKey, monthKey) = periodKeys
    val weeklyTasks by taskRepository.observeInstancesByPeriod(weekKey).collectAsState(initial = emptyList())
    val monthlyTasks by taskRepository.observeInstancesByPeriod(monthKey).collectAsState(initial = emptyList())

    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteTargetTag by remember { mutableStateOf<Tag?>(null) }

    // 删除任务实例确认
    var deleteTarget by remember { mutableStateOf<TaskInstance?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                syncService.sync()
                periodKeys = currentPeriodKeys()
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
            // bodyDiv：可滚动，3 个卡片垂直排列
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 周任务 card
                HomeTaskCard(
                    title = "📅 本周任务",
                    instances = weeklyTasks,
                    onToggle = { inst -> scope.launch { taskRepository.toggleDone(inst) } },
                    onDelete = { inst -> deleteTarget = inst },
                )

                // 月任务 card
                HomeTaskCard(
                    title = "📆 本月任务",
                    instances = monthlyTasks,
                    onToggle = { inst -> scope.launch { taskRepository.toggleDone(inst) } },
                    onDelete = { inst -> deleteTarget = inst },
                )

                // 记一笔（宽矮 card）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                ) {
                    Text(
                        "记一笔",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (tags.isEmpty()) {
                            Text("还没有标签，去管理页创建一个吧", style = MaterialTheme.typography.bodySmall)
                        }
                        tags.take(6).forEach { tag ->
                            QuickCheckInButton(
                                tag = tag,
                                onClick = {
                                    noteTargetTag = tag
                                    showNoteDialog = true
                                },
                            )
                        }
                    }
                }

                // 底部安全距离
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    // 删除任务实例确认弹窗
    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("永久删除「${target.title}」？\n此操作无法恢复。") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { taskRepository.deleteInstance(target) }
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
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

/**
 * 任务区块：标题 + 任务项列表（带勾选框）。
 */
/**
 * 分区卡片：标题 + 进度徽章 + 任务列表 + 进度条。
 */
@Composable
private fun HomeTaskCard(
    title: String,
    instances: List<TaskInstance>,
    onToggle: (TaskInstance) -> Unit,
    onDelete: (TaskInstance) -> Unit,
) {
    val doneCount = instances.count { it.done }
    val totalCount = instances.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // 标题行：标题 + 进度徽章
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "$doneCount/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (instances.isEmpty()) {
            Text("暂无任务", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        // 任务列表
        instances.forEach { instance ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = instance.done,
                    onCheckedChange = { onToggle(instance) },
                )
                Text(
                    text = instance.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (instance.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // 删除按钮
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "删除",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete(instance) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }

        // 进度条
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/**
 * 快捷打卡：小纯色圆圈（无文字），一行 6 个。
 */
@Composable
private fun QuickCheckInButton(tag: Tag, onClick: () -> Unit) {
    val tagColor = Color(tag.color)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tagColor)
                .clickable(onClick = onClick),
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
