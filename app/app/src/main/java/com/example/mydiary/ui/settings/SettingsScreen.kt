package com.example.mydiary.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.mydiary.data.DataTransferService
import com.example.mydiary.data.SleepReminderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页：JSON/SQL 导出、导入、睡前提醒开关。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    transferService: DataTransferService,
    reminderService: SleepReminderService,
    scope: CoroutineScope,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(reminderService.isEnabled()) }

    // 导入文件选择器：按按钮来源标记类型，不依赖 URI 猜测
    var pendingImportType by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: android.net.Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: throw Exception("无法读取文件内容")
                val count = if (pendingImportType == "sql") {
                    transferService.importSql(text)
                } else {
                    transferService.importJson(text)
                }
                statusMessage = "导入成功，共 $count 条记录"
            } catch (e: Exception) {
                statusMessage = "导入失败：${e.message}"
            }
        }
    }

    fun pickImportFile(importType: String, mimeTypes: Array<String>) {
        pendingImportType = importType
        importLauncher.launch(mimeTypes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // 导出
            Text("导出", style = MaterialTheme.typography.titleSmall)

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = transferService.exportJson()
                            val filename = "mydiary_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                            saveToDownload(context, filename, json, "application/json")
                            statusMessage = "JSON 导出成功"
                            Toast.makeText(context, "已保存到 Download 文件夹", Toast.LENGTH_SHORT).show()
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
                            val filename = "mydiary_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.sql"
                            saveToDownload(context, filename, sql, "application/sql")
                            statusMessage = "SQL 导出成功"
                            Toast.makeText(context, "已保存到 Download 文件夹", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            statusMessage = "导出失败：${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出 SQL") }

            // 导入
            Button(
                onClick = { pickImportFile("json", arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入 JSON") }

            Button(
                onClick = { pickImportFile("sql", arrayOf("application/sql", "text/plain")) },
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


/**
 * 直接保存到系统 Download/MyDiary 子文件夹（MediaStore，API 29+ 无需权限）。
 */
private fun saveToDownload(context: android.content.Context, filename: String, content: String, mimeType: String) {
    val resolver = context.contentResolver
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
        // Android 11+ 只允许 Download 目录，不允许自定义子目录
        put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download")
    }
    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: throw Exception("无法创建 Download 文件")
    resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        ?: throw Exception("无法写入 Download 文件")
}
