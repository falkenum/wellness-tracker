package com.sjfalken.wellnesstracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "reminder_channel"
        val channelName = "Reminder Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)

        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(channel)

        val reminderType = (intent.extras?.get(MainActivity.BundleKeys.REMINDER_TYPE) as? String) ?: "[Empty]"
        val notifIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.BundleKeys.NEW_ENTRY_TYPE, reminderType)
        }

        val reminderId = (intent.extras?.get(MainActivity.BundleKeys.REMINDER_ID) as? Int) ?: 0
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle("Wellness Reminder")
            .setContentIntent(
                PendingIntent.getActivity(context, 0, notifIntent, 0)
            )
            .setContentText("Time to record a $reminderType")
            .setAutoCancel(true)
            .build()


        notifManager.notify(reminderId, notification)
    }
}
