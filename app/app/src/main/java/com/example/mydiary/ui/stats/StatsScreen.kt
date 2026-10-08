package com.example.mydiary.ui.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.CheckIn
import com.example.mydiary.data.CheckInRepository
import com.example.mydiary.data.StatsAggregator
import com.example.mydiary.data.Tag
import com.example.mydiary.data.TagRepository
import com.example.mydiary.data.TaskInstance
import com.example.mydiary.data.TaskRepository
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * 统计页：周/月/年/自定义 × 按标签聚合 + 任务完成统计（completedAt）+ 打卡明细弹窗。
 */
@Composable
fun StatsScreen(
    checkInRepository: CheckInRepository,
    tagRepository: TagRepository,
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val checkIns by checkInRepository.observeAll().collectAsState(initial = emptyList())
    val tags by tagRepository.observeAll().collectAsState(initial = emptyList())
    val allInstances by taskRepository.observeAllInstances().collectAsState(initial = emptyList())
    val templates by taskRepository.observeTemplates().collectAsState(initial = emptyList())
    var granularity by remember { mutableStateOf(StatsAggregator.Granularity.WEEK) }
    var detailTagId by remember { mutableStateOf<Int?>(null) }

    // 自定义时间范围
    var customStart by remember { mutableStateOf<Long?>(null) }
    var customEnd by remember { mutableStateOf<Long?>(null) }
    var showCustomPicker by remember { mutableStateOf(false) }

    val tagMap = remember(tags) { tags.associateBy { it.id } }
    val tagNames = remember(tags) { tags.associate { it.id to it.name } }
    val templateNames = remember(templates) { templates.associate { it.id to it.name } }
    val reference = LocalDate.now()

    // 计算时间范围：自定义 or 周/月/年
    val (rangeStart, rangeEnd) = remember(granularity, reference, customStart, customEnd) {
        if (granularity == StatsAggregator.Granularity.CUSTOM && customStart != null && customEnd != null) {
            Pair(customStart!!, customEnd!!)
        } else {
            StatsAggregator.rangeFor(granularity, reference)
        }
    }

    val results = remember(checkIns, tags, rangeStart, rangeEnd) {
        StatsAggregator.aggregateByRange(checkIns, tagNames, rangeStart, rangeEnd)
    }

    // 每个标签在统计范围内的最近打卡时间
    val latestByTag = remember(checkIns, rangeStart, rangeEnd) {
        checkIns
            .filter { it.timestamp in rangeStart..rangeEnd && it.tagId != null }
            .groupBy { it.tagId!! }
            .mapValues { (_, events) -> events.maxOf { it.timestamp } }
    }

    // 任务完成统计：按模板分组，显示子事件计数（统一用 completedAt）
    val taskStats = remember(allInstances, rangeStart, rangeEnd, templates) {
        allInstances
            .filter { it.done && (it.completedAt ?: 0L) in rangeStart..rangeEnd }
            .groupBy { it.templateId }
            .map { (tplId, items) ->
                Triple(
                    templateNames[tplId] ?: "未知任务",
                    items.size,
                    items.groupBy { it.title }.mapValues { it.value.size }.entries
                        .sortedByDescending { it.value }
                        .map { (k, v) -> Pair(k, v) }
                        .toList(),
                )
            }
            .sortedByDescending { it.second }
    }

    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("统计", style = MaterialTheme.typography.headlineSmall)

            // 粒度切换（周/月/年/自定义）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsAggregator.Granularity.entries.forEach { g ->
                    FilterChip(
                        selected = granularity == g,
                        onClick = {
                            granularity = g
                            if (g == StatsAggregator.Granularity.CUSTOM) showCustomPicker = true
                        },
                        label = {
                            Text(when (g) {
                                StatsAggregator.Granularity.WEEK -> "周"
                                StatsAggregator.Granularity.MONTH -> "月"
                                StatsAggregator.Granularity.YEAR -> "年"
                                StatsAggregator.Granularity.CUSTOM -> "日期"
                            })
                        },
                    )
                }
            }

            // 时间范围标签
            Text(
                text = if (granularity == StatsAggregator.Granularity.CUSTOM && customStart != null && customEnd != null) {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    "${fmt.format(Date(customStart!!))} ~ ${fmt.format(Date(customEnd!!))}"
                } else {
                    StatsAggregator.periodLabel(granularity, reference)
                },
                style = MaterialTheme.typography.titleMedium,
            )

            // 打卡聚合结果（可点击查看明细）
            if (results.isEmpty()) {
                Text("这个时间段还没有记录", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { detailTagId = item.tagId },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 颜色小圆圈
                                val tag: Tag? = item.tagId?.let { tagMap[it] }
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (tag != null) Color(tag.color)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                )
                                Text(
                                    item.tagName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                // 最近时间 + 总次数
                                val latestTs = item.tagId?.let { latestByTag[it] }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (latestTs != null) {
                                        Text(
                                            timeFormat.format(Date(latestTs)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        "共 ${item.count} 次",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 任务完成统计区块（按模板分组，二级列表）
            TaskCompletionSection(data = taskStats)
        }
    }

    // 自定义日期选择器
    if (showCustomPicker) {
        CustomDateDialog(
            initialStart = customStart,
            initialEnd = customEnd,
            onConfirm = { start, end ->
                customStart = start
                customEnd = end
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false },
        )
    }

    // 明细弹窗
    if (detailTagId != null) {
        val detailRecords = remember(checkIns, detailTagId, rangeStart, rangeEnd) {
            checkIns
                .filter { it.tagId == detailTagId && it.timestamp in rangeStart..rangeEnd }
                .sortedByDescending { it.timestamp }
        }
        CheckInDetailSheet(
            tagName = detailTagId?.let { tagNames[it] } ?: "已删除标签",
            tagColor = detailTagId?.let { tagMap[it]?.color },
            records = detailRecords,
            onDismiss = { detailTagId = null },
            onDelete = { record -> scope.launch { checkInRepository.delete(record) } },
        )
    }
}

/**
 * 任务完成统计：按模板分组，一级=任务名+总次，二级=子事件+次数。
 */
@Composable
private fun TaskCompletionSection(
    data: List<Triple<String, Int, List<Pair<String, Int>>>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("任务完成", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (data.isEmpty()) {
            Text("这个时间段还没有完成任务", style = MaterialTheme.typography.bodyMedium)
            return
        }

        data.forEach { (taskName, total, items) ->
            // 一级：任务名 + 总次数
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(taskName, style = MaterialTheme.typography.bodyLarge)
                Text("$total 次", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            // 二级：子事件列表
            items.forEach { (itemName, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(itemName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$count 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 自定义时间范围选择对话框（Material 3 DatePicker）。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    // 当前步骤：0=选开始，1=选结束
    var step by remember { mutableStateOf(0) }
    var selectedStart by remember { mutableStateOf(initialStart?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now().withDayOfMonth(1)) }
    var selectedEnd by remember { mutableStateOf(initialEnd?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 开始日期
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("开始：${startFmt.format(Date(selectedStart.atStartOfDay(zone).toInstant().toEpochMilli()))}", modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { showStartPicker = true }) { Text("修改") }
                }
                // 结束日期
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("结束：${startFmt.format(Date(selectedEnd.atStartOfDay(zone).toInstant().toEpochMilli()))}", modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { showEndPicker = true }) { Text("修改") }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = {
                val s = selectedStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = selectedEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() // 包含结束日当天
                onConfirm(s, e)
            }) { Text("确定") }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )

    // 开始日期选择器
    if (showStartPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDate = selectedStart)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    pickerState.selectedDateMillis?.let { selectedStart = Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showStartPicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = pickerState) }
    }

    // 结束日期选择器
    if (showEndPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDate = selectedEnd)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    pickerState.selectedDateMillis?.let { selectedEnd = Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showEndPicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun CheckInDetailSheet(
    tagName: String,
    tagColor: Int?,
    records: List<CheckIn>,
    onDismiss: () -> Unit,
    onDelete: (CheckIn) -> Unit = {},
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val todayStart = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    var deleteTarget by remember { mutableStateOf<CheckIn?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 颜色小圆圈
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (tagColor != null) Color(tagColor)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                )
                Text(
                    "${tagName} 的明细 (${records.size} 条)",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        },
        text = {
            if (records.isEmpty()) {
                Text("暂无记录")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    records.forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = dateFormat.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // "今"标记
                            if (record.timestamp >= todayStart) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("今", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = record.note ?: "无备注",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            // 删除按钮
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                                contentDescription = "删除",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { deleteTarget = record },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onDismiss) { Text("关闭") }
        },
    )

    // 删除确认
    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这条打卡记录？\n${dateFormat.format(Date(target.timestamp))}") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}
