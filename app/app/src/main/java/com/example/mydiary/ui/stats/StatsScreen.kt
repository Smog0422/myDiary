package com.example.mydiary.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.mydiary.data.currentPeriodKeys
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * 统计页：周/月/年 × 按标签聚合 + 周期数据（已完成子事件）+ 打卡明细弹窗。
 */
@Composable
fun StatsScreen(
    checkInRepository: CheckInRepository,
    tagRepository: TagRepository,
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
) {
    val checkIns by checkInRepository.observeAll().collectAsState(initial = emptyList())
    val tags by tagRepository.observeAll().collectAsState(initial = emptyList())
    val allInstances by taskRepository.observeAllInstances().collectAsState(initial = emptyList())
    var granularity by remember { mutableStateOf(StatsAggregator.Granularity.WEEK) }
    var detailTagId by remember { mutableStateOf<Int?>(null) }

    val tagMap = remember(tags) { tags.associateBy { it.id } }
    val tagNames = remember(tags) { tags.associate { it.id to it.name } }
    val reference = LocalDate.now()
    val (currentWeekKey, currentMonthKey) = remember { currentPeriodKeys(reference) }

    val results = remember(checkIns, tags, granularity) {
        StatsAggregator.aggregate(checkIns, tagNames, granularity, reference)
    }

    // 每个标签在统计范围内的最近打卡时间
    val (rangeStart, rangeEnd) = remember(granularity, reference) {
        StatsAggregator.rangeFor(granularity, reference)
    }
    val latestByTag = remember(checkIns, rangeStart, rangeEnd) {
        checkIns
            .filter { it.timestamp in rangeStart..rangeEnd && it.tagId != null }
            .groupBy { it.tagId!! }
            .mapValues { (_, events) -> events.maxOf { it.timestamp } }
    }

    // 周期数据：排除当前周期的已完成实例，按粒度前缀分组
    val periodPrefix = remember(granularity, reference) {
        when (granularity) {
            StatsAggregator.Granularity.WEEK -> currentWeekKey.substringBefore("-W")
            StatsAggregator.Granularity.MONTH -> currentMonthKey
            StatsAggregator.Granularity.YEAR -> reference.year.toString()
        }
    }
    val periodData = remember(allInstances, granularity, periodPrefix) {
        allInstances
            .filter { it.done && !it.periodKey.startsWith(periodPrefix) }
            .groupBy { inst: TaskInstance ->
                when (granularity) {
                    StatsAggregator.Granularity.WEEK -> inst.periodKey.substringBeforeLast("-W") + "-W" + inst.periodKey.substringAfterLast("W")
                    StatsAggregator.Granularity.MONTH -> inst.periodKey
                    StatsAggregator.Granularity.YEAR -> inst.periodKey.substring(0, 4)
                }
            }
            .entries
            .sortedByDescending { it.key }
            .map { entry: Map.Entry<String, List<TaskInstance>> -> entry.key to entry.value }
    }

    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val todayStart = remember { LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }

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

            // 粒度切换
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsAggregator.Granularity.entries.forEach { g ->
                    FilterChip(
                        selected = granularity == g,
                        onClick = { granularity = g },
                        label = {
                            Text(when (g) {
                                StatsAggregator.Granularity.WEEK -> "周"
                                StatsAggregator.Granularity.MONTH -> "月"
                                StatsAggregator.Granularity.YEAR -> "年"
                            })
                        },
                    )
                }
            }

            // 当前周期标签
            Text(
                StatsAggregator.periodLabel(granularity, reference),
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
                                // 最近时间 + "今"标记 + 总次数
                                val latestTs = item.tagId?.let { latestByTag[it] }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (latestTs != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                timeFormat.format(Date(latestTs)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            // "今"标记：小圆圈包裹汉字
                                            if (latestTs >= todayStart) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        "今",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                    )
                                                }
                                            }
                                        }
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

            // 周期数据区块
            PeriodDataSection(data = periodData)
        }
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
        )
    }
}

/**
 * 周期数据：按粒度分组显示已完成子事件（名称 + 次数 + 所属任务）。
 */
@Composable
private fun PeriodDataSection(
    data: List<Pair<String, List<TaskInstance>>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("周期数据", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (data.isEmpty()) {
            Text("暂无往期完成记录", style = MaterialTheme.typography.bodyMedium)
            return
        }

        data.forEach { (periodKey, instances) ->
            Text(
                periodKey,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            instances.forEach { inst ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("✓", color = MaterialTheme.colorScheme.primary)
                    Text(
                        inst.title,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CheckInDetailSheet(
    tagName: String,
    tagColor: Int?,
    records: List<CheckIn>,
    onDismiss: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = dateFormat.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = record.note ?: "无备注",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
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
}
