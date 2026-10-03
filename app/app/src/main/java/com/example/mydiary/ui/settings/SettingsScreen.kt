package com.example.mydiary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.mydiary.data.ImportValidationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 设置页：JSON 导出/导入。
 */
@Composable
fun SettingsScreen(
    transferService: DataTransferService,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val json = transferService.exportJson()
                            // 通过 ShareSheet 分享文件
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                java.io.File(context.filesDir, "mydiary_export.json").apply {
                                    writeText(json)
                                }
                            )
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                                context.startActivity(
                                    android.content.Intent.createChooser(intent, "导出 MyDiary 数据")
                                )
                            statusMessage = "导出成功"
                        } catch (e: Exception) {
                            statusMessage = "导出失败：${e.message ?: "未知错误"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("导出数据（JSON）")
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            // 简化：从剪贴板或文件选择器读取
                            // 完整实现需要 ActivityResultContracts
                            statusMessage = "导入功能需要文件选择器（后续完善）"
                        } catch (e: ImportValidationException) {
                            statusMessage = e.message ?: "导入失败"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("导入数据（JSON）")
            }

            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
