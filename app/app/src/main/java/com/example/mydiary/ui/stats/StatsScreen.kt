package com.example.mydiary.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.CheckInRepository
import com.example.mydiary.data.StatsAggregator
import com.example.mydiary.data.TagRepository
import com.example.mydiary.data.TaskInstance
import com.example.mydiary.data.TaskRepository
import com.example.mydiary.data.currentPeriodKeys
import java.time.LocalDate

/**
 * 统计页：周/月/年 × 按标签聚合 + 任务历史完成率。
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

    val tagNames = remember(tags) { tags.associate { it.id to it.name } }
    val reference = LocalDate.now()
    val (currentWeekKey, currentMonthKey) = remember { currentPeriodKeys(reference) }

    val results = remember(checkIns, tags, granularity) {
        StatsAggregator.aggregate(checkIns, tagNames, granularity, reference)
    }

    // 任务历史：排除当前周/月的过期实例，按 periodKey 降序分组
    val history = remember(allInstances, currentWeekKey, currentMonthKey) {
        allInstances
            .filter { it.periodKey != currentWeekKey && it.periodKey != currentMonthKey }
            .groupBy { it.periodKey }
            .entries
            .sortedByDescending { it.key }
            .map { (key, instances) -> key to instances }
    }

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

            // 打卡聚合结果
            if (results.isEmpty()) {
                Text("这个时间段还没有记录", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.tagName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    "${item.count} 次",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 任务历史区块
            TaskHistorySection(history = history)
        }
    }
}
