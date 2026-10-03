package com.example.mydiary.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.Tag
import com.example.mydiary.data.TagRepository
import kotlinx.coroutines.launch
/**
 * 管理页：标签的增删改 + 颜色选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    repository: TagRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tags by repository.observeAll().collectAsState(initial = emptyList())
    var showColorPicker by remember { mutableStateOf(false) }
    var pickerTargetId by remember { mutableStateOf<Int?>(null) }
    var pickerColor by remember { mutableStateOf(Color.Blue) }
    var newTagName by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("标签管理", style = MaterialTheme.typography.headlineSmall)

            // 新建标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("新标签名称") },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newTagName.isNotBlank()) {
                        scope.launch {
                            repository.create(newTagName.trim(), Color.Blue.value.toInt())
                            newTagName = ""
                        }
                    }
                }) {
                    Text("添加")
                }
            }

            // 标签列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (tags.isEmpty()) {
                    Text("还没有标签，先创建一个吧", style = MaterialTheme.typography.bodyMedium)
                }
                tags.forEach { tag ->
                    TagRow(
                        tag = tag,
                        onRename = { name -> scope.launch { repository.rename(tag.id, name) } },
                        onDelete = { scope.launch { repository.delete(tag.id) } },
                        onPickColor = {
                            pickerTargetId = tag.id
                            pickerColor = Color(tag.color)
                            showColorPicker = true
                        },
                    )
                }
            }
        }
    }

    // 颜色选择对话框
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = pickerColor,
            onConfirm = { color ->
                val targetId = pickerTargetId
                showColorPicker = false
                if (targetId != null) {
                    scope.launch { repository.recolor(targetId, color.value.toInt()) }
                }
            },
            onDismiss = { showColorPicker = false },
        )
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onPickColor: () -> Unit,
) {
    var editing by remember(tag.id, tag.name) { mutableStateOf(false) }
    var draft by remember(tag.name) { mutableStateOf(tag.name) }

    Column {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 颜色圆点
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(tag.color)),
                )

                if (editing) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = {
                        if (draft.isNotBlank()) onRename(draft.trim())
                        editing = false
                    }) { Text("保存") }
                } else {
                    Text(
                        text = tag.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { editing = true; draft = tag.name }) {
                        Text("改名")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onPickColor) { Text("换色") }
            OutlinedButton(onClick = onDelete) { Text("删除") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val presetColors = listOf(
        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xAB47BC),
        Color(0xFF5E35B1), Color(0xFF3F51B5), Color(0xFF2196F3),
        Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688),
        Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
        Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722),
        Color(0xFF795548), Color(0xFF607D8B), Color(0xFF9E9E9E),
    )
    var selected by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column {
                for (i in 0 until presetColors.size step 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (j in 0 until minOf(6, presetColors.size - i)) {
                            val color = presetColors[i + j]
                            ColorSwatch(color = color, isSelected = selected == color, onClick = { selected = color })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    )
}
