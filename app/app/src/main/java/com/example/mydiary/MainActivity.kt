package com.example.mydiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.mydiary.data.CheckInRepository
import com.example.mydiary.data.CheckInRepositoryImpl
import com.example.mydiary.data.MyDiaryDatabase
import com.example.mydiary.data.TagRepository
import com.example.mydiary.data.TagRepositoryImpl
import com.example.mydiary.ui.home.HomeScreen
import com.example.mydiary.ui.tags.TagManagementScreen
import com.example.mydiary.ui.theme.MyDiaryTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(this, MyDiaryDatabase::class.java, "mydiary.db")
            .addMigrations(MyDiaryDatabase.MIGRATION_1_2)
            .build()
        val tagRepository = TagRepositoryImpl(database.tagDao())
        val checkInRepository = CheckInRepositoryImpl(database.checkInDao())

        setContent {
            MyDiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScaffold(
                        tagRepository = tagRepository,
                        checkInRepository = checkInRepository,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    tagRepository: TagRepository,
    checkInRepository: CheckInRepository,
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = "管理") },
                    label = { Text("管理") },
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                tagRepository = tagRepository,
                checkInRepository = checkInRepository,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> TagManagementScreen(
                repository = tagRepository,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
