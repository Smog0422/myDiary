package com.example.mydiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.mydiary.data.MyDiaryDatabase
import com.example.mydiary.data.TagRepository
import com.example.mydiary.data.TagRepositoryImpl
import com.example.mydiary.ui.tags.TagManagementScreen
import com.example.mydiary.ui.theme.MyDiaryTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: MyDiaryDatabase
    private lateinit var tagRepository: TagRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = Room.databaseBuilder(this, MyDiaryDatabase::class.java, "mydiary.db")
            .build()
        tagRepository = TagRepositoryImpl(database.tagDao(), database)

        setContent {
            MyDiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TagManagementScreen(repository = tagRepository)
                }
            }
        }
    }
}
