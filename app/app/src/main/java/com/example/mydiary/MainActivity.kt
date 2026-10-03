package com.example.mydiary

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.mydiary.ui.theme.MyDiaryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(this, MyDiaryDatabase::class.java, "mydiary.db")
            .addMigrations(MyDiaryDatabase.MIGRATION_1_2, MyDiaryDatabase.MIGRATION_2_3)
            .build()
        val tagRepository = TagRepositoryImpl(database.tagDao())
        val checkInRepository = CheckInRepositoryImpl(database.checkInDao())
        val taskRepository = TaskRepositoryImpl(
            database.taskTemplateDao(),
            database.taskItemDao(),
            database.taskInstanceDao(),
        )
        val transferService = DataTransferService(tagRepository, checkInRepository, taskRepository)
        val reminderService = SleepReminderService(this)

        // T4: App 启动时自动同步缺失周期
        val syncService = PeriodSyncService(
            taskRepository = taskRepository,
            templateProvider = { taskRepository.observeTemplates().first() },
        )
        lifecycleScope.launch { syncService.sync() }

        setContent {
            MyDiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScaffold(tagRepository, checkInRepository, taskRepository, transferService, reminderService)
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    tagRepository: com.example.mydiary.data.TagRepository,
    checkInRepository: com.example.mydiary.data.CheckInRepository,
    taskRepository: com.example.mydiary.data.TaskRepository,
    transferService: DataTransferService,
    reminderService: SleepReminderService,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
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
