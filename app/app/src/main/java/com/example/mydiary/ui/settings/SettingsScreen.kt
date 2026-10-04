package com.example.mydiary.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.DataTransferService
import com.example.mydiary.data.SleepReminderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 设置页：JSON/SQL 导出、导入、睡前提醒开关。
 */
@Composable
fun SettingsScreen(
    transferService: DataTransferService,
    reminderService: SleepReminderService,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(reminderService.isEnabled()) }

    // 导入文件选择器（json / sql）
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: android.net.Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: throw Exception("无法读取文件内容")
                val count = when {
                    uri.path?.lowercase()?.endsWith(".sql") == true -> transferService.importSql(text)
                    else -> transferService.importJson(text)
                }
                statusMessage = "导入成功，共 $count 条记录"
            } catch (e: Exception) {
                statusMessage = "导入失败：${e.message}"
            }
        }
    }

    fun pickImportFile(mimeTypes: Array<String>) {
        importLauncher.launch(mimeTypes)
    }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.titleMedium)

            // 导出
            Text("导出", style = MaterialTheme.typography.titleSmall)

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = transferService.exportJson()
                            shareFile(context, "mydiary_export.json", json, "application/json")
                            statusMessage = "JSON 导出成功"
                        } catch (e: Exception) {
                            statusMessage = "导出失败：${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出 JSON") }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val sql = transferService.exportSql()
                            shareFile(context, "mydiary_export.sql", sql, "application/sql")
                            statusMessage = "SQL 导出成功"
                        } catch (e: Exception) {
                            statusMessage = "导出失败：${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出 SQL") }

            // 导入
            Button(
                onClick = { pickImportFile(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入 JSON") }

            Button(
                onClick = { pickImportFile(arrayOf("application/sql", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入 SQL") }

            // 睡前提醒
            Text("提醒", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    "睡前提醒（默认 22:00）",
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        reminderEnabled = enabled
                        reminderService.setEnabled(enabled)
                    },
                )
            }

            // 状态消息
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun shareFile(context: android.content.Context, filename: String, content: String, mimeType: String) {
    val file = java.io.File(context.filesDir, filename)
    file.writeText(content)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "分享 $filename"))
}
