package com.example.mydiary.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mydiary.data.TagRepository
import com.example.mydiary.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 管理页：双栏 Tab（标签 / 任务）。
 */
@Composable
fun TagManagementScreen(
    repository: TagRepository,
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("标签", modifier = Modifier.padding(horizontal = 16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("任务", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        when (selectedTab) {
            0 -> TagTabContent(repository = repository)
            1 -> TaskTabContent(taskRepository = taskRepository)
        }
    }
}

// ===== 标签 Tab =====

@Composable
private fun TagTabContent(repository: TagRepository) {
    val scope = rememberCoroutineScope()
    val tags by repository.observeAll().collectAsState(initial = emptyList())
    var colorPickerTag by remember { mutableStateOf<com.example.mydiary.data.Tag?>(null) }
    var showNewTagDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 纯色圆点（无文字）
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(tag.color)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(tag.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                // 换色按钮
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "换色",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { colorPickerTag = tag },
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 删除按钮
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { scope.launch { repository.delete(tag.id) } },
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        // 新增标签按钮
        Button(
            onClick = { showNewTagDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("＋ 新增标签")
        }

        // 新增标签弹窗
        if (showNewTagDialog) {
            NewTagDialog(
                onConfirm = { name, color ->
                    scope.launch { repository.create(name, color) }
                    showNewTagDialog = false
                },
                onDismiss = { showNewTagDialog = false },
            )
        }

        // 颜色选择对话框
        colorPickerTag?.let { tag ->
            ColorPickerDialog(
                onConfirm = { color -> scope.launch { repository.recolor(tag.id, color) } },
                onDismiss = { colorPickerTag = null },
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = listOf(
        0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
        0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFFFD54F.toInt(),
        0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFA1887F.toInt(), 0xFF90A4AE.toInt(),
    )
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column {
                colors.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { onConfirm(color); onDismiss() },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        },
    )
}

// ===== 任务 Tab =====

@Composable
private fun TaskTabContent(taskRepository: TaskRepository) {
    val scope = rememberCoroutineScope()
    val allTemplates by taskRepository.observeTemplates().collectAsState(initial = emptyList())
    // 只显示非隐藏模板
    val visibleTemplates = allTemplates.filter { !it.hidden }
    val hiddenCount = allTemplates.count { it.hidden }

    var showConfigDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<com.example.mydiary.data.TaskTemplate?>(null) }
    var deleteTarget by remember { mutableStateOf<com.example.mydiary.data.TaskTemplate?>(null) }
    var showHiddenList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 任务卡片列表（仅非隐藏）
        visibleTemplates.forEach { template ->
            TaskListItemCard(
                template = template,
                onClick = {
                    editingTemplate = template
                    showConfigDialog = true
                },
                onToggleHide = { scope.launch { taskRepository.setHidden(template.id, true) } },
                onDelete = { deleteTarget = template },
            )
        }

        // 添加按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(12.dp)
                .clickable {
                    editingTemplate = null
                    showConfigDialog = true
                },
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("+ 添加任务模板", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 已隐藏按钮
        if (hiddenCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(12.dp)
                    .clickable { showHiddenList = true },
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("已隐藏 ($hiddenCount)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // 配置弹窗
    if (showConfigDialog) {
        TemplateConfigDialog(
            template = editingTemplate,
            taskRepository = taskRepository,
            onDismiss = {
                showConfigDialog = false
                editingTemplate = null
            },
        )
    }

    // 删除确认弹窗
    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("确认删除「${target.name}」？该行为无法恢复。\n（已生成的任务实例不受影响）") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { taskRepository.deleteTemplate(target.id) }
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    // 已隐藏列表弹窗
    if (showHiddenList) {
        HiddenTemplatesDialog(
            hiddenTemplates = allTemplates.filter { it.hidden },
            onRestore = { t -> scope.launch { taskRepository.setHidden(t.id, false) } },
            onDelete = { t -> deleteTarget = t; showHiddenList = false },
            onDismiss = { showHiddenList = false },
        )
    }
}

/**
 * 已隐藏模板列表弹窗：可恢复或删除。
 */
@Composable
private fun HiddenTemplatesDialog(
    hiddenTemplates: List<com.example.mydiary.data.TaskTemplate>,
    onRestore: (com.example.mydiary.data.TaskTemplate) -> Unit,
    onDelete: (com.example.mydiary.data.TaskTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已隐藏的任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hiddenTemplates.isEmpty()) {
                    Text("没有隐藏的任务", style = MaterialTheme.typography.bodyMedium)
                }
                hiddenTemplates.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = t.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // 恢复按钮
                        Text(
                            text = "恢复",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRestore(t) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // 删除按钮
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDelete(t) },
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 低矮横向任务卡片：图标 + 名称 + 状态icon。
 */
@Composable
private fun TaskListItemCard(
    template: com.example.mydiary.data.TaskTemplate,
    onClick: () -> Unit,
    onToggleHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val isWeekly = template.period == "weekly"
    val iconColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 可点击区域：图标 + 名称 + 状态
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = if (template.hidden) 0.4f else 1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isWeekly) "周" else "月",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 名称 + 状态
            Column {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (template.hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 开启/停止
                    StatusBadge(text = if (template.active) "✓" else "⏸", active = template.active)
                    // 追溯
                    if (template.backfill) StatusBadge(text = "追溯", active = true, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // 眼睛图标（隐藏/显示）— 在删除左侧，留足距离防误触
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = if (template.hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = if (template.hidden) "显示" else "隐藏",
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onToggleHide),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 删除按钮（最右端）
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "删除",
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StatusBadge(text: String, active: Boolean, color: Color? = null) {
    val badgeColor = color ?: MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(if (active) badgeColor.copy(alpha = 0.15f) else Color(0xFF999999).copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 配置弹窗：名称 + 类型Select + 触发日/追溯/开启同一行 + 任务项列表。
 */
@Composable
private fun TemplateConfigDialog(
    template: com.example.mydiary.data.TaskTemplate?,
    taskRepository: TaskRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // 所有状态以 template 为 key：弹窗每次打开都从数据库真实状态重新初始化，
    // 避免 Composable 重组时残留旧值（导致 checkbox"没保存"、保存按钮灰色）
    var name by remember(template) { mutableStateOf(template?.name ?: "") }
    var period by remember(template) { mutableStateOf(template?.period ?: "weekly") }
    var triggerDay by remember(template) { mutableIntStateOf(template?.triggerDay ?: 1) }
    var backfill by remember(template) { mutableStateOf(template?.backfill ?: false) }
    var active by remember(template) { mutableStateOf(template?.active ?: true) }
    var hidden by remember(template) { mutableStateOf(template?.hidden ?: false) }
    var items by remember(template) { mutableStateOf<List<com.example.mydiary.data.TaskItem>>(emptyList()) }
    var loadedItems by remember(template) { mutableStateOf<List<com.example.mydiary.data.TaskItem>>(emptyList()) }
    var newItemText by remember(template) { mutableStateOf("") }

    // 加载已有任务项
    LaunchedEffect(template) {
        if (template != null) {
            val loaded = taskRepository.observeItems(template.id).first()
            items = loaded
            loadedItems = loaded
        }
    }

    val maxDay = if (period == "weekly") 7 else 28
    val dayLabels = if (period == "weekly") {
        listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    } else {
        (1..28).map { "${it}号" }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "添加任务模板" else "配置任务模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 名称
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称") },
                    singleLine = true,
                )

                // 类型 + 触发日：同一行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeriodSelectButton(label = "周", selected = period == "weekly", onClick = { period = "weekly" })
                    PeriodSelectButton(label = "月", selected = period == "monthly", onClick = { period = "monthly" })
                    // 触发日 Dropdown
                    var showDayMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { showDayMenu = true }) {
                            Text(dayLabels[triggerDay - 1])
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showDayMenu,
                            onDismissRequest = { showDayMenu = false },
                        ) {
                            dayLabels.forEachIndexed { index, label ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        triggerDay = index + 1
                                        showDayMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                // 追溯 + 开启：同一行（隐藏通过卡片眼睛图标操作）
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = backfill,
                            onCheckedChange = { backfill = it },
                        )
                        Text("追溯", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = active,
                            onCheckedChange = { active = it },
                        )
                        Text("开启", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 任务项列表
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { items = items.filter { it.id != item.id } },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // 添加任务项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("添加任务项") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newItemText.isNotEmpty()) {
                            items = items + com.example.mydiary.data.TaskItem(
                                id = 0, templateId = 0, title = newItemText, sortOrder = (items.maxOfOrNull { it.sortOrder } ?: 0) + 10
                            )
                            newItemText = ""
                        }
                    }) { Text("+") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    scope.launch {
                        val trimmedName = name.trim()
                        val clampedDay = triggerDay.coerceIn(1, maxDay)

                        if (template == null) {
                            // ===== 新建：insert 模板（带所有字段）→ insert 任务项 =====
                            val newTemplate = com.example.mydiary.data.TaskTemplate(
                                id = 0,
                                period = period,
                                name = trimmedName,
                                triggerDay = clampedDay,
                                backfill = backfill,
                                active = active,
                                hidden = false,
                            )
                            val tid = taskRepository.insertTemplate(newTemplate).toInt()

                            // 任务项数组
                            items.forEachIndexed { index, item ->
                                taskRepository.insertItem(
                                    com.example.mydiary.data.TaskItem(
                                        id = 0, templateId = tid, title = item.title, sortOrder = (index + 1) * 10
                                    )
                                )
                            }
                            // 不调 syncAfterSave：实例生成由 App 启动时的 PeriodSyncService.sync() 统一处理
                        } else {
                            // ===== 编辑：事务写入（update 模板 + 删旧项 + 插新项）=====
                            val updatedTemplate = template.copy(
                                name = trimmedName,
                                period = period,
                                triggerDay = clampedDay,
                                backfill = backfill,
                                active = active,
                                // hidden 不变（通过卡片眼睛图标操作）
                            )

                            val keepIds = items.filter { it.id > 0 }.map { it.id }
                            val itemsToDelete = loadedItems.filter { it.id !in keepIds }

                            val maxSortOfKept = (items.filter { it.id > 0 }.maxOfOrNull { it.sortOrder } ?: 0)
                            val itemsToInsert = items.filter { it.id == 0 }.mapIndexed { index, item ->
                                com.example.mydiary.data.TaskItem(
                                    id = 0, templateId = template.id, title = item.title, sortOrder = maxSortOfKept + (index + 1) * 10
                                )
                            }

                            taskRepository.saveTemplateWithItems(updatedTemplate, itemsToDelete, itemsToInsert)
                            // 不调 syncAfterSave：实例生成由 App 启动时的 PeriodSyncService.sync() 统一处理
                        }
                        // 保存完成后再关闭弹窗，避免 coroutine 被取消导致数据未写完
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun PeriodSelectButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 新增标签弹窗：输入名字（2字）+ 选择颜色。
 */
@Composable
private fun NewTagDialog(
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF64B5F6.toInt()) }

    val colors = listOf(
        0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
        0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFFFD54F.toInt(),
        0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFA1887F.toInt(), 0xFF90A4AE.toInt(),
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 名字输入
                TextField(
                    value = name,
                    onValueChange = { name = it.take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("名称（2字）") },
                    singleLine = true,
                )

                // 颜色选择（2行8列，自适应填满宽度）
                Text("选择颜色", style = MaterialTheme.typography.bodySmall)
                Column {
                    colors.chunked(8).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .then(
                                            if (color == selectedColor)
                                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { selectedColor = color },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColor) },
                enabled = name.length == 2,
            ) { Text("创建") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
