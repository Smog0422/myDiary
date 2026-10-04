package com.example.mydiary

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mydiary.data.CheckInRepositoryImpl
import com.example.mydiary.data.DataTransferService
import com.example.mydiary.data.MyDiaryDatabase
import com.example.mydiary.data.PeriodSyncService
import com.example.mydiary.data.SleepReminderService
import com.example.mydiary.data.TagRepositoryImpl
import com.example.mydiary.data.TaskRepositoryImpl
import com.example.mydiary.ui.home.HomeScreen
import com.example.mydiary.ui.settings.SettingsScreen
import com.example.mydiary.ui.stats.StatsScreen
import com.example.mydiary.ui.tags.TagManagementScreen
import com.example.mydiary.ui.theme.AppTheme
import com.example.mydiary.ui.theme.BackgroundGradient
import com.example.mydiary.ui.theme.MyDiaryTheme
import com.example.mydiary.ui.theme.getThemeColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(this, MyDiaryDatabase::class.java, "mydiary_v2.db")
            .fallbackToDestructiveMigration()
            .build()
        val tagRepository = TagRepositoryImpl(database.tagDao())
        val checkInRepository = CheckInRepositoryImpl(database.checkInDao())
        val taskRepository = TaskRepositoryImpl(
            database,
            database.taskTemplateDao(),
            database.taskItemDao(),
            database.taskInstanceDao(),
        )
        val transferService = DataTransferService(tagRepository, checkInRepository, taskRepository)
        val reminderService = SleepReminderService(this)

        val syncService = PeriodSyncService(
            taskRepository = taskRepository,
            templateProvider = { taskRepository.observeTemplates().first() },
        )
        lifecycleScope.launch { syncService.sync() }

        setContent {
            var themeFlag by rememberSaveable { mutableIntStateOf(getSavedTheme(this)) }
            var darkFlag by rememberSaveable { mutableIntStateOf(if (isDarkMode(this)) 1 else 0) }

            val appTheme = when (themeFlag) {
                1 -> AppTheme.WARM_SAND
                2 -> AppTheme.PALE_SEA
                else -> AppTheme.MORNING_MIST
            }
            val darkTheme = darkFlag == 1

            MyDiaryTheme(theme = appTheme, darkTheme = darkTheme) {
                val themeColors = getThemeColors(appTheme, darkTheme)
                BackgroundGradient(colors = themeColors) {
                    Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Transparent) {
                        MainScaffold(
                            tagRepository = tagRepository,
                            checkInRepository = checkInRepository,
                            taskRepository = taskRepository,
                            syncService = syncService,
                            transferService = transferService,
                            reminderService = reminderService,
                            appTheme = appTheme,
                            darkTheme = darkTheme,
                            onToggleDarkTheme = { darkFlag = if (darkFlag == 1) 0 else 1 },
                            onSelectTheme = { t -> themeFlag = t },
                        )
                    }
                }
            }
        }
    }

    private fun getSavedTheme(context: Context): Int {
        return context.getSharedPreferences("mydiary", Context.MODE_PRIVATE)
            .getInt("theme", 0)
    }

    private fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences("mydiary", Context.MODE_PRIVATE)
            .getBoolean("dark_theme", false)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    tagRepository: com.example.mydiary.data.TagRepository,
    checkInRepository: com.example.mydiary.data.CheckInRepository,
    taskRepository: com.example.mydiary.data.TaskRepository,
    syncService: PeriodSyncService,
    transferService: DataTransferService,
    reminderService: SleepReminderService,
    appTheme: AppTheme,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onSelectTheme: (Int) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showThemeMenu by remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("MyDiary", style = MaterialTheme.typography.titleLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    // 主题切换
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "切换主题")
                    }
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        DropdownMenuItem(text = { Text("晨雾 · 淡绿") }, onClick = { onSelectTheme(0); showThemeMenu = false })
                        DropdownMenuItem(text = { Text("暖砂 · 米黄") }, onClick = { onSelectTheme(1); showThemeMenu = false })
                        DropdownMenuItem(text = { Text("淡海 · 雾蓝") }, onClick = { onSelectTheme(2); showThemeMenu = false })
                    }
                    // 日/夜切换
                    IconButton(onClick = onToggleDarkTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkTheme) "切换白天" else "切换黑夜",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                    label = { Text("首页") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "统计") },
                    label = { Text("统计") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "管理") },
                    label = { Text("管理") },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "设置") },
                    label = { Text("设置") },
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                tagRepository = tagRepository,
                checkInRepository = checkInRepository,
                taskRepository = taskRepository,
                syncService = syncService,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> StatsScreen(
                checkInRepository = checkInRepository,
                tagRepository = tagRepository,
                taskRepository = taskRepository,
                modifier = Modifier.padding(innerPadding),
            )
            2 -> TagManagementScreen(
                repository = tagRepository,
                taskRepository = taskRepository,
                modifier = Modifier.padding(innerPadding),
            )
            3 -> SettingsScreen(
                transferService = transferService,
                reminderService = reminderService,
                scope = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycleScope,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
