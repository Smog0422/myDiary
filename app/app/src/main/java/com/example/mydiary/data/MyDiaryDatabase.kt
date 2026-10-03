package com.example.mydiary.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mydiary.data.TagDao
import com.example.mydiary.data.Tag

@Database(
    entities = [Tag::class],
    version = 1,
    exportSchema = true,
)
abstract class MyDiaryDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
}
