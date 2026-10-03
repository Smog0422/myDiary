package com.example.mydiary.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.TaskInstance

/**
 * 任务历史区块：列出过期周期的完成率 + 漏掉的具体项。
 */
@Composable
fun TaskHistorySection(
    history: List<Pair<String, List<TaskInstance>>>, // periodKey to instances
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("任务历史", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text("暂无过期周期", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            return
        }

        history.forEach { (periodKey, instances) ->
            val total = instances.size
            val done = instances.count { it.done }
            val missed = instances.filter { !it.done }.map { it.title }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$periodKey：完成 $done/$total",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                )
                if (missed.isNotEmpty()) {
                    Text(
                        text = "漏了：${missed.joinToString("、")}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Red,
                    )
                }
            }
        }
    }
}
