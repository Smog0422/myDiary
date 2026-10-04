package com.example.mydiary.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Tag::class, CheckIn::class, TaskTemplate::class, TaskItem::class, TaskInstance::class],
    version = 5,
    exportSchema = true,
)
abstract class MyDiaryDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun checkInDao(): CheckInDao
    abstract fun taskTemplateDao(): TaskTemplateDao
    abstract fun taskItemDao(): TaskItemDao
    abstract fun taskInstanceDao(): TaskInstanceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `check_ins` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tagId` INTEGER,
                        `note` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY (`tagId`) REFERENCES `tags` (`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_tagId` ON `check_ins` (`tagId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_timestamp` ON `check_ins` (`timestamp`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `task_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `period` TEXT NOT NULL,
                        `name` TEXT NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `task_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `templateId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY (`templateId`) REFERENCES `task_templates` (`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_items_templateId` ON `task_items` (`templateId`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `task_instances` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `templateId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `done` INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_instances_templateId_periodKey` ON `task_instances` (`templateId`, `periodKey`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `task_templates` ADD COLUMN `triggerDay` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `task_templates` ADD COLUMN `backfill` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `task_templates` ADD COLUMN `active` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `task_templates` ADD COLUMN `hidden` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 去掉 (templateId, periodKey) 唯一索引：一个模板在一个周期可以有多个实例（每个子事件一条）
                db.execSQL("DROP INDEX IF EXISTS `index_task_instances_templateId_periodKey`")
            }
        }
    }
}
