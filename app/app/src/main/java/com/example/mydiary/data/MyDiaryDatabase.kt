package com.example.mydiary.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Tag::class, CheckIn::class],
    version = 2,
    exportSchema = true,
)
abstract class MyDiaryDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `check_ins` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tagId` INTEGER,
                        `note` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY (`tagId`) REFERENCES `tags` (`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_tagId` ON `check_ins` (`tagId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_timestamp` ON `check_ins` (`timestamp`)")
            }
        }
    }
}
