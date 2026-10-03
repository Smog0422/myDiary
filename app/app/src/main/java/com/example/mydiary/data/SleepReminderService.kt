package com.example.mydiary.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * 睡前提醒：发送系统通知。
 */
class SleepReminderService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "sleep_reminder"
        const val NOTIFICATION_ID = 1
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "睡前提醒", NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun sendReminder() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MyDiary")
            .setContentText("看看今天任务完成了没")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    fun isEnabled(): Boolean = false
    fun setEnabled(enabled: Boolean) { /* 简化：实际存 SharedPreferences */ }
}
